package com.pengcheng.message.inbox;

/**
 * 站内信 WebSocket 桥接接口（E5 SPI）
 *
 * <p>解耦 WebInboxSender（pengcheng-message）与 MessageWebSocketHandler（pengcheng-admin-api）。
 * 后者在 API 层注册实现 Bean 并桥接到 {@code MessageWebSocketHandler.sendNotice}。
 *
 * <p>若 Bean 未注入（API 层未启动），{@link WebInboxSender} 仍可正常落库，
 * 仅跳过实时推送。
 */
public interface InboxWebSocketBridge {

    /**
     * 如果用户在线，发送系统通知到 WebSocket
     *
     * @param userId  接收方用户 ID
     * @param title   通知标题
     * @param content 通知内容
     */
    void sendNoticeIfOnline(Long userId, String title, String content);

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return true 表示 WebSocket 会话活跃
     */
    boolean isOnline(Long userId);
}
