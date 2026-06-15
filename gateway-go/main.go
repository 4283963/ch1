package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/mux"
)

type AnchorPool struct {
	mu    sync.RWMutex
	conns map[string]net.Conn
}

func NewAnchorPool() *AnchorPool {
	return &AnchorPool{
		conns: make(map[string]net.Conn),
	}
}

func (p *AnchorPool) Add(spaceId string, conn net.Conn) {
	p.mu.Lock()
	defer p.mu.Unlock()
	if oldConn, exists := p.conns[spaceId]; exists {
		oldConn.Close()
	}
	p.conns[spaceId] = conn
}

func (p *AnchorPool) Remove(spaceId string) {
	p.mu.Lock()
	defer p.mu.Unlock()
	delete(p.conns, spaceId)
}

func (p *AnchorPool) Get(spaceId string) (net.Conn, bool) {
	p.mu.RLock()
	defer p.mu.RUnlock()
	conn, ok := p.conns[spaceId]
	return conn, ok
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

	if strings.TrimSpace(req.SpaceId) == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: "spaceId 不能为空"})
		return
	}

	conn, ok := h.pool.Get(req.SpaceId)
	if !ok {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: fmt.Sprintf("车位 %s 的地锚不在线", req.SpaceId)})
		return
	}

	if err := conn.SetWriteDeadline(time.Now().Add(5 * time.Second)); err != nil {
		log.Printf("[TCP] 设置写入超时失败: %v", err)
	}

	if _, err := fmt.Fprintf(conn, "%s\n", cmd); err != nil {
		h.pool.Remove(req.SpaceId)
		conn.Close()
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: fmt.Sprintf("发送指令失败: %v", err)})
		return
	}

	reader := bufio.NewReader(conn)
	if err := conn.SetReadDeadline(time.Now().Add(5 * time.Second)); err != nil {
		log.Printf("[TCP] 设置读取超时失败: %v", err)
	}

	resp, err := reader.ReadString('\n')
	if err != nil {
		h.pool.Remove(req.SpaceId)
		conn.Close()
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: fmt.Sprintf("等待ACK超时或失败: %v", err)})
		return
	}

	resp = strings.TrimSpace(resp)
	if resp != "ACK:OK" {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(APIResponse{Success: false, Message: fmt.Sprintf("地锚返回异常: %s", resp)})
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(APIResponse{Success: true, Message: fmt.Sprintf("%s 指令发送成功", cmd)})
}

func (h *AnchorHandler) HandleAnchorStatus(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	status := h.pool.Status()
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(APIResponse{
		Success: true,
		Data: map[string]interface{}{
			"total":  len(status),
			"online": status,
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

	log.Printf("[TCP] 服务启动，监听端口 %s", s.addr)

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

	s.connMu.Lock()
	s.connMap[conn] = spaceId
	s.connMu.Unlock()
	s.pool.Add(spaceId, conn)

	log.Printf("[TCP] 地锚 %s 注册成功 (来自 %s)", spaceId, conn.RemoteAddr())

	if err := conn.SetReadDeadline(time.Time{}); err != nil {
		log.Printf("[TCP] 清除读取超时失败: %v", err)
	}

	for {
		if err := conn.SetReadDeadline(time.Now().Add(60 * time.Second)); err != nil {
			log.Printf("[TCP] 设置心跳超时失败: %v", err)
			return
		}

		line, err := reader.ReadString('\n')
		if err != nil {
			if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
				continue
			}
			return
		}

		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		if line == "PING" {
			if err := conn.SetWriteDeadline(time.Now().Add(5 * time.Second)); err != nil {
				log.Printf("[TCP] 设置写入超时失败: %v", err)
			}
			if _, err := fmt.Fprintf(conn, "PONG\n"); err != nil {
				log.Printf("[TCP] 发送PONG失败: %v", err)
				return
			}
			continue
		}

		log.Printf("[TCP] 收到地锚 %s 消息: %s", spaceId, line)
	}
}

func main() {
	pool := NewAnchorPool()

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
