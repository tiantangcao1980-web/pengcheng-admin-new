package com.pengcheng.system.doc.collab.ws;

import com.pengcheng.system.doc.collab.service.DocCollabService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 文档协作房间注册表
 * 按 docId 索引所有活跃 Room，并负责定时持久化脏数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocCollabRegistry {

    private final DocCollabService docCollabService;

    /** docId → 协作房间 */
    private final ConcurrentHashMap<Long, DocCollabRoom> rooms = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    /** 持久化间隔（秒） */
    private static final int PERSIST_INTERVAL_SECONDS = 30;

    /** Presence 广播间隔（秒） */
    private static final int PRESENCE_INTERVAL_SECONDS = 5;

    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(2);

        // 每 30 秒将各 room 的脏数据落库
        scheduler.scheduleAtFixedRate(this::persistDirtyRooms,
                PERSIST_INTERVAL_SECONDS, PERSIST_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 每 5 秒广播各 room 的在线用户列表
        scheduler.scheduleAtFixedRate(this::broadcastPresence,
                PRESENCE_INTERVAL_SECONDS, PRESENCE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("[DocCollabRegistry] 定时任务启动：持久化 {}s / presence {}s",
                PERSIST_INTERVAL_SECONDS, PRESENCE_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * 获取（或创建）指定 docId 的协作房间
     */
    public DocCollabRoom getOrCreateRoom(Long docId) {
        return rooms.computeIfAbsent(docId, DocCollabRoom::new);
    }

    /**
     * 获取指定 docId 的协作房间，不存在则返回 null
     */
    public DocCollabRoom getRoom(Long docId) {
        return rooms.get(docId);
    }

    /**
     * 当房间变为空时移除（所有用户已断线）
     */
    public void removeRoomIfEmpty(Long docId) {
        rooms.computeIfPresent(docId, (id, room) -> room.isEmpty() ? null : room);
    }

    /**
     * 定时任务：将所有有脏数据的房间写入数据库
     */
    private void persistDirtyRooms() {
        rooms.forEach((docId, room) -> {
            if (room.checkAndClearDirty()) {
                try {
                    byte[] update = room.getLatestUpdate();
                    byte[] sv = room.getLatestStateVector();
                    Long updaterId = room.getLastUpdaterId();
                    if (update != null) {
                        docCollabService.persistUpdate(docId, sv, update, updaterId);
                        log.debug("[DocCollabRegistry] 持久化 docId={} 成功", docId);
                    }
                } catch (Exception e) {
                    log.error("[DocCollabRegistry] 持久化 docId={} 异常", docId, e);
                }
            }
        });
    }

    /**
     * 定时任务：向每个 room 广播在线用户 presence
     */
    private void broadcastPresence() {
        rooms.forEach((docId, room) -> {
            if (!room.isEmpty()) {
                try {
                    String payload = buildPresencePayload(room);
                    room.broadcastAll(payload);
                } catch (Exception e) {
                    log.warn("[DocCollabRegistry] presence 广播异常 docId={}", docId, e);
                }
            }
        });
    }

    private String buildPresencePayload(DocCollabRoom room) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"presence\",\"users\":[");
        boolean first = true;
        for (DocCollabSession s : room.getOnlineUsers()) {
            if (!first) sb.append(",");
            sb.append("{\"userId\":").append(s.getUserId())
              .append(",\"userName\":\"").append(escapeJson(s.getUserName())).append("\"")
              .append(",\"joinedAt\":\"").append(s.getJoinedAt()).append("\"}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** 供测试使用：获取当前所有 room 数量 */
    public int getRoomCount() {
        return rooms.size();
    }
}
