package com.pengcheng.message.inbox;

import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.push.unified.ChannelInboxSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Web 站内信 Sender（E5）
 *
 * <p>实现 {@link ChannelInboxSender}：
 * <ol>
 *   <li>通过 {@link NotificationService#createNotification} 落库</li>
 *   <li>若用户在线，通过 {@link InboxWebSocketBridge} 同步推送 WebSocket 消息</li>
 * </ol>
 *
 * <p>WebSocket bridge 为可选依赖（nullable），离线用户仅落库，不报错。
 */
@Slf4j
@RequiredArgsConstructor
public class WebInboxSender implements ChannelInboxSender {

    public static final String CHANNEL_CODE = "inbox";

    private final NotificationService notificationService;

    /**
     * WebSocket 桥接器（可为 null，表示无实时推送能力）
     */
    private final InboxWebSocketBridge webSocketBridge;

    /** 渠道编码（非接口方法 — ChannelInboxSender 接口未约束）。 */
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean send(String userId, String title, String content, String bizType, Long bizId) {
        // 1. 落库
        Long uid;
        try {
            uid = Long.parseLong(userId);
        } catch (Exception e) {
            log.warn("[Inbox] userId 非法: {}", userId);
            return false;
        }
        try {
            Notification notification = Notification.builder()
                    .userId(uid)
                    .title(title)
                    .content(content)
                    .bizType(bizType)
                    .bizId(bizId)
                    .build();
            notificationService.createNotification(notification);
            log.debug("[Inbox] 站内信落库成功: userId={}, title={}", uid, title);
        } catch (Exception e) {
            log.error("[Inbox] 站内信落库失败: userId={}, title={}, error={}", uid, title, e.getMessage(), e);
            return false;
        }

        // 2. 在线推送（fire-and-forget，不影响落库结果）
        if (webSocketBridge != null) {
            try {
                webSocketBridge.sendNoticeIfOnline(uid, title, content);
            } catch (Exception e) {
                log.warn("[Inbox] WebSocket 推送失败（已忽略）: userId={}, error={}", uid, e.getMessage());
            }
        }

        return true;
    }
}
