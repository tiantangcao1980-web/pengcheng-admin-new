package com.pengcheng.system.doc.collab.ws;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单个文档的协作房间
 * 维护在线用户列表并提供广播能力
 * CRDT 合并由前端 Y.js 完成，服务端仅做广播 + 落库
 */
@Slf4j
public class DocCollabRoom {

    @Getter
    private final Long docId;

    /** sessionId → WebSocket 连接 */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** sessionId → 用户元信息 */
    private final Map<String, DocCollabSession> sessionMeta = new ConcurrentHashMap<>();

    /**
     * 标记自上次持久化以来是否有新的 update 到达
     * Registry 的定时任务会检查此标志后执行落库
     */
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    /**
     * 最新 update blob（base64），由客户端 push 后覆盖
     * 持久化时使用此值
     */
    @Getter
    private volatile byte[] latestUpdate;

    @Getter
    private volatile byte[] latestStateVector;

    @Getter
    private volatile Long lastUpdaterId;

    public DocCollabRoom(Long docId) {
        this.docId = docId;
    }

    public void join(WebSocketSession ws, DocCollabSession meta) {
        sessions.put(ws.getId(), ws);
        sessionMeta.put(ws.getId(), meta);
        log.info("[Room-{}] 用户 {} 加入，当前在线 {} 人", docId, meta.getUserId(), sessions.size());
    }

    public void leave(String sessionId) {
        sessions.remove(sessionId);
        DocCollabSession meta = sessionMeta.remove(sessionId);
        if (meta != null) {
            log.info("[Room-{}] 用户 {} 离开，剩余 {} 人", docId, meta.getUserId(), sessions.size());
        }
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    public Collection<DocCollabSession> getOnlineUsers() {
        return sessionMeta.values();
    }

    /**
     * 广播消息给 room 内所有其他用户（排除发送方）
     */
    public void broadcastExclude(String excludeSessionId, String payload) {
        TextMessage msg = new TextMessage(payload);
        sessions.forEach((sid, ws) -> {
            if (!sid.equals(excludeSessionId) && ws.isOpen()) {
                try {
                    ws.sendMessage(msg);
                } catch (IOException e) {
                    log.warn("[Room-{}] 发送失败 session={}", docId, sid);
                }
            }
        });
    }

    /**
     * 广播给所有人（含发送方），用于 presence 推送
     */
    public void broadcastAll(String payload) {
        TextMessage msg = new TextMessage(payload);
        sessions.forEach((sid, ws) -> {
            if (ws.isOpen()) {
                try {
                    ws.sendMessage(msg);
                } catch (IOException e) {
                    log.warn("[Room-{}] 广播失败 session={}", docId, sid);
                }
            }
        });
    }

    /**
     * 向单个 session 发送消息（用于 sync_init 响应）
     */
    public void sendTo(String sessionId, String payload) {
        WebSocketSession ws = sessions.get(sessionId);
        if (ws != null && ws.isOpen()) {
            try {
                ws.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.warn("[Room-{}] 单发失败 session={}", docId, sessionId);
            }
        }
    }

    /**
     * 收到客户端 update 时调用，标记 dirty 并缓存最新状态
     */
    public void receiveUpdate(byte[] updateBlob, byte[] stateVector, Long updaterId) {
        this.latestUpdate = updateBlob;
        this.latestStateVector = stateVector;
        this.lastUpdaterId = updaterId;
        this.dirty.set(true);
    }

    /**
     * 检查并重置 dirty 标志，返回是否需要持久化
     */
    public boolean checkAndClearDirty() {
        return dirty.compareAndSet(true, false);
    }

    public List<Map<String, Object>> getOnlineUserList() {
        List<Map<String, Object>> list = new ArrayList<>();
        sessionMeta.values().forEach(s -> list.add(Map.of(
                "userId", s.getUserId(),
                "userName", s.getUserName(),
                "joinedAt", s.getJoinedAt().toString()
        )));
        return list;
    }
}
