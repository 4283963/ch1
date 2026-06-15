package main

import (
	"bufio"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/mux"
)

const (
	commandQueueSize    = 16
	anchorCooldownMs    = 2000
	tcpWriteTimeoutSec  = 5
	tcpReadTimeoutSec   = 5
	tcpHeartbeatSec     = 60
)

type commandItem struct {
	cmd     string
	ackChan chan error
}

type AnchorConnection struct {
	spaceId         string
	conn            net.Conn
	writer          *bufio.Writer
	reader          *bufio.Reader
	cmdQueue        chan commandItem
	lastCmdTime     int64
	lastCmd         string
	pendingCmdCount int
	closeChan       chan struct{}
	writeMu         sync.Mutex
	workerMu        sync.Mutex
	workerRunning   bool
}

func NewAnchorConnection(spaceId string, conn net.Conn) *AnchorConnection {
	return &AnchorConnection{
		spaceId:   spaceId,
		conn:      conn,
		writer:    bufio.NewWriterSize(conn, 128),
		reader:    bufio.NewReaderSize(conn, 128),
		cmdQueue:  make(chan commandItem, commandQueueSize),
		closeChan: make(chan struct{}),
	}
}

func (a *AnchorConnection) StartWorker() {
	a.workerMu.Lock()
	if a.workerRunning {
		a.workerMu.Unlock()
		return
	}
	a.workerRunning = true
	a.workerMu.Unlock()

	go a.commandWorker()
	log.Printf("[Worker] 地锚 %s 指令消费协程已启动", a.spaceId)
}

func (a *AnchorConnection) Stop() {
	select {
	case <-a.closeChan:
		return
	default:
		close(a.closeChan)
	}
	a.conn.Close()
}

func (a *AnchorConnection) commandWorker() {
	defer func() {
		a.workerMu.Lock()
		a.workerRunning = false
		a.workerMu.Unlock()
		log.Printf("[Worker] 地锚 %s 指令消费协程已退出", a.spaceId)
	}()

	for {
		select {
		case <-a.closeChan:
			return

		case item, ok := <-a.cmdQueue:
			if !ok {
				return
			}
			err := a.executeCommand(item.cmd)
			if item.ackChan != nil {
				select {
				case item.ackChan <- err:
				case <-a.closeChan:
				}
			}
			a.lastCmdTime = time.Now().UnixMilli()
			a.lastCmd = item.cmd
		}
	}
}

func (a *AnchorConnection) executeCommand(cmd string) error {
	a.writeMu.Lock()
	defer a.writeMu.Unlock()

	log.Printf("[TCP] 地锚 %s 执行指令: %s", a.spaceId, cmd)

	if err := a.conn.SetWriteDeadline(time.Now().Add(tcpWriteTimeoutSec * time.Second)); err != nil {
		log.Printf("[TCP] 地锚 %s 设置写超时失败: %v", a.spaceId, err)
	}

	if _, err := a.writer.WriteString(cmd + "\n"); err != nil {
		log.Printf("[TCP] 地锚 %s 写入指令失败: %v", a.spaceId, err)
		a.writer.Reset(a.conn)
		return err
	}

	if err := a.writer.Flush(); err != nil {
		log.Printf("[TCP] 地锚 %s Flush失败: %v", a.spaceId, err)
		return err
	}

	if err := a.conn.SetReadDeadline(time.Now().Add(tcpReadTimeoutSec * time.Second)); err != nil {
		log.Printf("[TCP] 地锚 %s 设置读超时失败: %v", a.spaceId, err)
	}

	resp, err := a.reader.ReadString('\n')
	if err != nil {
		log.Printf("[TCP] 地锚 %s 读取ACK失败: %v", a.spaceId, err)
		return err
	}

	resp = strings.TrimSpace(resp)
	if resp != "ACK:OK" {
		log.Printf("[TCP] 地锚 %s 返回异常: %q (期望 ACK:OK)", a.spaceId, resp)
		return errors.New("地锚返回异常: " + resp)
	}

	log.Printf("[TCP] 地锚 %s 指令 %s 执行成功 (ACK:OK)", a.spaceId, cmd)
	return nil
}

func (a *AnchorConnection) EnqueueCommand(cmd string, waitAck bool) error {
	a.writeMu.Lock()
	lastTime := a.lastCmdTime
	lastCmd := a.lastCmd
	pending := len(a.cmdQueue)
	a.writeMu.Unlock()

	now := time.Now().UnixMilli()
	if lastTime > 0 && (now-lastTime) < anchorCooldownMs {
		remain := anchorCooldownMs - (now - lastTime)
		log.Printf("[队列-冷却] 地锚 %s 距上次指令仅%dms，冷却拒绝(剩%dms)",
			a.spaceId, now-lastTime, remain)
		return fmt.Errorf("液压泵冷却中，请%dms后重试(防止电机过载)", remain)
	}

	if pending > 0 && lastCmd == cmd {
		log.Printf("[队列-去抖] 地锚 %s 队列中已有相同指令[%s](待执行%d条)，合并跳过",
			a.spaceId, cmd, pending)
		return fmt.Errorf("队列中已有相同指令，已合并去抖")
	}

	if len(a.cmdQueue) >= commandQueueSize {
		log.Printf("[队列-满] 地锚 %s 指令队列已满(size=%d)，拒绝新指令", a.spaceId, commandQueueSize)
		return errors.New("地锚指令队列已满，请稍后重试")
	}

	a.StartWorker()

	var ackChan chan error
	if waitAck {
		ackChan = make(chan error, 1)
	}

	select {
	case a.cmdQueue <- commandItem{cmd: cmd, ackChan: ackChan}:
		log.Printf("[队列-入队] 地锚 %s 指令 %s 入队成功(队列长度=%d)",
			a.spaceId, cmd, len(a.cmdQueue))
	default:
		log.Printf("[队列-满] 地锚 %s 入队失败，channel已满", a.spaceId)
		return errors.New("地锚繁忙，请稍后重试")
	}

	if waitAck {
		select {
		case err := <-ackChan:
			return err
		case <-time.After((tcpWriteTimeoutSec + tcpReadTimeoutSec + 2) * time.Second):
			log.Printf("[队列-超时] 地锚 %s 指令 %s 等待ACK超时", a.spaceId, cmd)
			return errors.New("地锚响应超时")
		case <-a.closeChan:
			return errors.New("地锚连接已断开")
		}
	}
	return nil
}

type AnchorPool struct {
	mu    sync.RWMutex
	conns map[string]*AnchorConnection
}

func NewAnchorPool() *AnchorPool {
	return &AnchorPool{
		conns: make(map[string]*AnchorConnection),
	}
}

func (p *AnchorPool) Add(spaceId string, ac *AnchorConnection) {
	p.mu.Lock()
	defer p.mu.Unlock()
	if old, exists := p.conns[spaceId]; exists {
		log.Printf("[Pool] 地锚 %s 重复注册，关闭旧连接", spaceId)
		old.Stop()
	}
	p.conns[spaceId] = ac
}

func (p *AnchorPool) Remove(spaceId string) {
	p.mu.Lock()
	defer p.mu.Unlock()
	if ac, ok := p.conns[spaceId]; ok {
		ac.Stop()
		delete(p.conns, spaceId)
	}
}

func (p *AnchorPool) Get(spaceId string) (*AnchorConnection, bool) {
	p.mu.RLock()
	defer p.mu.RUnlock()
	ac, ok := p.conns[spaceId]
	return ac, ok
}

func (p *AnchorPool) Status() map[string]bool {
	p.mu.RLock()
	defer p.mu.RUnlock()
	status := make(map[string]bool, len(p.conns))
	for id := range p.conns {
		status[id] = true
	}
	return status
}

func (p *AnchorPool) QueueStatus() map[string]interface{} {
	p.mu.RLock()
	defer p.mu.RUnlock()
	result := make(map[string]interface{}, len(p.conns))
	for id, ac := range p.conns {
		result[id] = map[string]interface{}{
			"queueLen":    len(ac.cmdQueue),
			"lastCmd":     ac.lastCmd,
			"lastCmdTime": ac.lastCmdTime,
		}
	}
	return result
}

type AnchorHandler struct {
	pool *AnchorPool
}

func NewAnchorHandler(pool *AnchorPool) *AnchorHandler {
	return &AnchorHandler{pool: pool}
}

type CommandRequest struct {
	SpaceId string `json:"spaceId"`
}

type APIResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message,omitempty"`
	Data    interface{} `json:"data,omitempty"`
}

type AnnounceRequest struct {
	SpeakerId string `json:"speakerId"`
	Text      string `json:"text"`
	Volume    int    `json:"volume"`
	Priority  int    `json:"priority"`
}

type AnnounceHandler struct {
	recentSpeak []map[string]interface{}
	mu          sync.Mutex
}

func NewAnnounceHandler() *AnnounceHandler {
	return &AnnounceHandler{
		recentSpeak: make([]map[string]interface{}, 0, 50),
	}
}

func (h *AnnounceHandler) HandleSpeak(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	var req AnnounceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: "无效的请求体: " + err.Error()})
		return
	}
	defer r.Body.Close()

	speakerId := strings.TrimSpace(req.SpeakerId)
	if speakerId == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: "speakerId 不能为空"})
		return
	}
	if req.Text == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: "播报文本不能为空"})
		return
	}

	log.Printf("[播报] 喇叭 %s 音量=%d 优先级=%d 文本=%q",
		speakerId, req.Volume, req.Priority, req.Text)

	record := map[string]interface{}{
		"speakerId": speakerId,
		"text":      req.Text,
		"volume":    req.Volume,
		"priority":  req.Priority,
		"timestamp": time.Now().Format("15:04:05"),
		"success":   true,
	}
	h.mu.Lock()
	h.recentSpeak = append([]map[string]interface{}{record}, h.recentSpeak...)
	if len(h.recentSpeak) > 50 {
		h.recentSpeak = h.recentSpeak[:50]
	}
	h.mu.Unlock()

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(APIResponse{
		Success: true,
		Message: fmt.Sprintf("喇叭 %s 播报指令已发送", speakerId),
		Data:    record,
	})
}

func (h *AnnounceHandler) HandleStatus(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	h.mu.Lock()
	recent := make([]map[string]interface{}, len(h.recentSpeak))
	copy(recent, h.recentSpeak)
	h.mu.Unlock()
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(APIResponse{
		Success: true,
		Data: map[string]interface{}{
			"totalAnnounced": len(recent),
			"recentRecords":  recent,
		},
	})
}

func (h *AnchorHandler) HandleAnchorUp(w http.ResponseWriter, r *http.Request) {
	h.sendCommand(w, r, "UP")
}

func (h *AnchorHandler) HandleAnchorDown(w http.ResponseWriter, r *http.Request) {
	h.sendCommand(w, r, "DOWN")
}

func (h *AnchorHandler) sendCommand(w http.ResponseWriter, r *http.Request, cmd string) {
	w.Header().Set("Content-Type", "application/json")

	var req CommandRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: "无效的请求体: " + err.Error()})
		return
	}
	defer r.Body.Close()

	spaceId := strings.TrimSpace(req.SpaceId)
	if spaceId == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: "spaceId 不能为空"})
		return
	}

	ac, ok := h.pool.Get(spaceId)
	if !ok {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: fmt.Sprintf("车位 %s 的地锚不在线", spaceId)})
		return
	}

	err := ac.EnqueueCommand(cmd, true)
	if err != nil {
		w.WriteHeader(http.StatusTooManyRequests)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: err.Error()})
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(APIResponse{Success: true, Message: fmt.Sprintf("%s 指令执行成功", cmd)})
}

func (h *AnchorHandler) HandleAnchorStatus(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	status := h.pool.Status()
	queues := h.pool.QueueStatus()
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(APIResponse{
		Success: true,
		Data: map[string]interface{}{
			"total":    len(status),
			"online":   status,
			"queues":   queues,
			"cooldown": fmt.Sprintf("%dms/指令", anchorCooldownMs),
		},
	})
}

type TCPServer struct {
	pool    *AnchorPool
	addr    string
	connMap map[net.Conn]string
	connMu  sync.Mutex
}

func NewTCPServer(addr string, pool *AnchorPool) *TCPServer {
	return &TCPServer{
		pool:    pool,
		addr:    addr,
		connMap: make(map[net.Conn]string),
	}
}

func (s *TCPServer) Start() error {
	listener, err := net.Listen("tcp", s.addr)
	if err != nil {
		return fmt.Errorf("TCP 监听失败: %w", err)
	}
	defer listener.Close()

	log.Printf("[TCP] 服务启动，监听端口 %s (指令冷却%dms/队列容量%d)",
		s.addr, anchorCooldownMs, commandQueueSize)

	for {
		conn, err := listener.Accept()
		if err != nil {
			log.Printf("[TCP] 接受连接失败: %v", err)
			continue
		}
		go s.handleConnection(conn)
	}
}

func (s *TCPServer) handleConnection(conn net.Conn) {
	defer func() {
		s.connMu.Lock()
		spaceId, ok := s.connMap[conn]
		if ok {
			delete(s.connMap, conn)
			s.pool.Remove(spaceId)
			log.Printf("[TCP] 地锚 %s 已断开连接", spaceId)
		}
		s.connMu.Unlock()
		conn.Close()
	}()

	log.Printf("[TCP] 新连接来自 %s", conn.RemoteAddr())

	reader := bufio.NewReader(conn)
	if err := conn.SetReadDeadline(time.Now().Add(10 * time.Second)); err != nil {
		log.Printf("[TCP] 设置读取超时失败: %v", err)
		return
	}

	firstLine, err := reader.ReadString('\n')
	if err != nil {
		log.Printf("[TCP] 读取注册消息失败: %v", err)
		return
	}

	firstLine = strings.TrimSpace(firstLine)
	if !strings.HasPrefix(firstLine, "REGISTER:") {
		log.Printf("[TCP] 无效的注册消息: %s", firstLine)
		return
	}

	spaceId := strings.TrimPrefix(firstLine, "REGISTER:")
	spaceId = strings.TrimSpace(spaceId)
	if spaceId == "" {
		log.Printf("[TCP] 注册消息缺少车位ID")
		return
	}

	ac := NewAnchorConnection(spaceId, conn)

	s.connMu.Lock()
	s.connMap[conn] = spaceId
	s.connMu.Unlock()
	s.pool.Add(spaceId, ac)

	ac.reader = reader
	ac.StartWorker()

	log.Printf("[TCP] 地锚 %s 注册成功 (来自 %s)，指令协程已启动", spaceId, conn.RemoteAddr())

	for {
		if err := conn.SetReadDeadline(time.Now().Add(tcpHeartbeatSec * time.Second)); err != nil {
			log.Printf("[TCP] 地锚 %s 设置心跳超时失败: %v", spaceId, err)
			return
		}

		line, err := reader.ReadString('\n')
		if err != nil {
			if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
				continue
			}
			log.Printf("[TCP] 地锚 %s 读循环退出: %v", spaceId, err)
			return
		}

		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		if line == "PING" {
			ac.writeMu.Lock()
			if err := conn.SetWriteDeadline(time.Now().Add(tcpWriteTimeoutSec * time.Second)); err != nil {
				log.Printf("[TCP] 地锚 %s 设置写超时失败: %v", spaceId, err)
			}
			if _, werr := fmt.Fprintf(conn, "PONG\n"); werr != nil {
				log.Printf("[TCP] 地锚 %s 发送PONG失败: %v", spaceId, werr)
				ac.writeMu.Unlock()
				return
			}
			ac.writeMu.Unlock()
			continue
		}

		log.Printf("[TCP] 收到地锚 %s 主动上报: %s", spaceId, line)
	}
}

func main() {
	pool := NewAnchorPool()
	announceHandler := NewAnnounceHandler()

	tcpServer := NewTCPServer(":9000", pool)
	go func() {
		if err := tcpServer.Start(); err != nil {
			log.Fatalf("TCP 服务启动失败: %v", err)
		}
	}()

	handler := NewAnchorHandler(pool)
	router := mux.NewRouter()

	apiRouter := router.PathPrefix("/api/anchor").Subrouter()
	apiRouter.HandleFunc("/up", handler.HandleAnchorUp).Methods("POST")
	apiRouter.HandleFunc("/down", handler.HandleAnchorDown).Methods("POST")
	apiRouter.HandleFunc("/status", handler.HandleAnchorStatus).Methods("GET")

	announceRouter := router.PathPrefix("/api/announce").Subrouter()
	announceRouter.HandleFunc("/speak", announceHandler.HandleSpeak).Methods("POST")
	announceRouter.HandleFunc("/status", announceHandler.HandleStatus).Methods("GET")

	httpAddr := ":8080"
	log.Printf("[HTTP] 服务启动，监听端口 %s", httpAddr)

	server := &http.Server{
		Addr:         httpAddr,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
	}

	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("HTTP 服务启动失败: %v", err)
	}
}
