package com.smartpark.websocket;

import com.alibaba.fastjson2.JSON;
import com.smartpark.dto.SpaceStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ParkingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ParkingWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket连接建立, sessionId={}, 当前连接数={}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到WebSocket消息, sessionId={}, message={}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket连接关闭, sessionId={}, status={}, 当前连接数={}", session.getId(), status, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误, sessionId={}", session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session);
    }

    public void sendSpaceStatusUpdate(Long spaceId, String spaceCode, String status, String color,
                                      Integer x, Integer y, Integer width, Integer height) {
        SpaceStatusDTO dto = new SpaceStatusDTO(spaceId, spaceCode, status, color, x, y, width, height);
        String jsonMessage = JSON.toJSONString(dto);
        broadcast(jsonMessage);
    }

    public void broadcast(String message) {
        log.debug("WebSocket广播消息: {}", message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("WebSocket发送消息失败, sessionId={}", session.getId(), e);
                }
            }
        }
    }

    public int getConnectionCount() {
        return sessions.size();
    }
}
