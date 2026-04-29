package com.pengcheng.admin.websocket;

import com.pengcheng.system.doc.collab.ws.DocCollabWebSocketHandler;
import com.pengcheng.websocket.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * 注册业务WebSocket处理器，容器配置由 pengcheng-websocket 模块提供
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;
    private final SshWebSocketHandler sshWebSocketHandler;
    /** Y.js 协同编辑 Handler（collab 子包，支持 /ws/doc/{docId} 路径） */
    private final DocCollabWebSocketHandler yjsDocCollabHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(MessageWebSocketHandler messageWebSocketHandler,
                          SshWebSocketHandler sshWebSocketHandler,
                          DocCollabWebSocketHandler yjsDocCollabHandler,
                          WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.sshWebSocketHandler = sshWebSocketHandler;
        this.yjsDocCollabHandler = yjsDocCollabHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 消息 WebSocket
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");

        // SSH 终端 WebSocket
        registry.addHandler(sshWebSocketHandler, "/ws/ssh")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");

        // 文档协同编辑 WebSocket（Y.js CRDT，/ws/doc/{docId}）
        // 兼容保留旧路径 /ws/doc，新路径 /ws/doc/* 由 yjsDocCollabHandler 处理
        registry.addHandler(yjsDocCollabHandler, "/ws/doc", "/ws/doc/*")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
