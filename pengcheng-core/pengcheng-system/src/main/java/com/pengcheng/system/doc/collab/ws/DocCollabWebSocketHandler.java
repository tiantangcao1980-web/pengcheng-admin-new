package com.pengcheng.system.doc.collab.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pengcheng.system.doc.collab.entity.DocCollabState;
import com.pengcheng.system.doc.collab.service.DocCollabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Base64;

/**
 * Y.js 文档实时协作 WebSocket Handler
 *
 * 协议消息（JSON text frame）：
 * - 客户端 → 服务端：
 *   {type:"sync_init"}             连接建立后发送，服务端回 snapshot
 *   {type:"update", sv:"<base64>", payload:"<base64>"}  Y.doc update
 *   {type:"awareness", payload:"<base64>"}              光标等弱状态
 *   {type:"join", userName:"xxx"}  加入 room（可选，未发时由服务端自动 join）
 *
 * - 服务端 → 客户端：
 *   {type:"snapshot", stateVector:"<base64>", updateBlob:"<base64>"}
 *   {type:"update", payload:"<base64>", userId:<long>}  广播给其他人
 *   {type:"awareness", payload:"<base64>", userId:<long>}
 *   {type:"presence", users:[{userId, userName, joinedAt}]}  定时 5s 广播
 *
 * 路径：/ws/doc/{docId}，docId 从 URI 末段解析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocCollabWebSocketHandler extends AbstractWebSocketHandler {

    private final DocCollabRegistry registry;
    private final DocCollabService docCollabService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long docId = extractDocId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        String userName = extractUserName(session);

        if (docId == null || userId == null) {
            log.warn("[DocCollab] 连接参数缺失，关闭 session={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        DocCollabRoom room = registry.getOrCreateRoom(docId);
        DocCollabSession meta = new DocCollabSession(docId, userId, session.getId(), userName);
        room.join(session, meta);

        log.info("[DocCollab] 用户 {} 连接文档 {} session={}", userId, docId, session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws Exception {
        Long docId = extractDocId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        if (docId == null || userId == null) return;

        DocCollabRoom room = registry.getRoom(docId);
        if (room == null) return;

        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.path("type").asText("");

        switch (type) {
            case "sync_init" -> handleSyncInit(session, room, docId);
            case "update" -> handleUpdate(session, room, json, userId);
            case "awareness" -> handleAwareness(session, room, json, userId);
            case "join" -> {
                // join 消息只更新 userName（已在 afterConnectionEstablished 加入 room）
                log.debug("[DocCollab] join 消息 userId={} docId={}", userId, docId);
            }
            default -> log.debug("[DocCollab] 未知消息类型 type={}", type);
        }
    }

    /**
     * 处理 sync_init：返回数据库中的最新快照给客户端
     */
    private void handleSyncInit(WebSocketSession session, DocCollabRoom room, Long docId) throws Exception {
        DocCollabState state = docCollabService.getSnapshot(docId);

        ObjectNode resp = objectMapper.createObjectNode();
        resp.put("type", "snapshot");
        if (state != null) {
            resp.put("stateVector", state.getStateVector() != null
                    ? Base64.getEncoder().encodeToString(state.getStateVector()) : "");
            resp.put("updateBlob", state.getUpdateBlob() != null
                    ? Base64.getEncoder().encodeToString(state.getUpdateBlob()) : "");
            resp.put("version", state.getVersion());
        } else {
            resp.put("stateVector", "");
            resp.put("updateBlob", "");
            resp.put("version", 0);
        }
        room.sendTo(session.getId(), objectMapper.writeValueAsString(resp));
    }

    /**
     * 处理 update：缓存最新 blob（标记 dirty）并广播给 room 内其他用户
     * 服务端不解码 CRDT，直接转发 base64 binary
     */
    private void handleUpdate(WebSocketSession session, DocCollabRoom room,
                              JsonNode json, Long userId) throws Exception {
        String payloadB64 = json.path("payload").asText("");
        String svB64 = json.path("sv").asText("");

        byte[] updateBlob = payloadB64.isEmpty() ? null : Base64.getDecoder().decode(payloadB64);
        byte[] stateVector = svB64.isEmpty() ? null : Base64.getDecoder().decode(svB64);

        if (updateBlob != null) {
            room.receiveUpdate(updateBlob, stateVector, userId);
        }

        // 广播给同 room 的其他用户
        ObjectNode broadcast = objectMapper.createObjectNode();
        broadcast.put("type", "update");
        broadcast.put("payload", payloadB64);
        broadcast.put("userId", userId);
        room.broadcastExclude(session.getId(), objectMapper.writeValueAsString(broadcast));
    }

    /**
     * 处理 awareness：光标/选区弱状态广播，服务端不持久化
     */
    private void handleAwareness(WebSocketSession session, DocCollabRoom room,
                                 JsonNode json, Long userId) throws Exception {
        String payloadB64 = json.path("payload").asText("");

        ObjectNode broadcast = objectMapper.createObjectNode();
        broadcast.put("type", "awareness");
        broadcast.put("payload", payloadB64);
        broadcast.put("userId", userId);
        room.broadcastExclude(session.getId(), objectMapper.writeValueAsString(broadcast));
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status) {
        Long docId = extractDocId(session);
        if (docId != null) {
            DocCollabRoom room = registry.getRoom(docId);
            if (room != null) {
                room.leave(session.getId());
                registry.removeRoomIfEmpty(docId);
            }
        }
        log.info("[DocCollab] 连接关闭 session={} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session,
                                     @NonNull Throwable exception) {
        log.warn("[DocCollab] 传输错误 session={} err={}", session.getId(), exception.getMessage());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 从 session URI 最后一段解析 docId
     * 路径形如 /ws/doc/123
     */
    private Long extractDocId(WebSocketSession session) {
        try {
            String path = session.getUri() != null ? session.getUri().getPath() : "";
            String[] parts = path.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("[DocCollab] 无法解析 docId from uri={}", session.getUri());
            return null;
        }
    }

    private String extractUserName(WebSocketSession session) {
        Object name = session.getAttributes().get("userName");
        return name != null ? name.toString() : "用户" + session.getAttributes().get("userId");
    }
}
