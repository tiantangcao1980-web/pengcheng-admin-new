package com.pengcheng.message.listener;

import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.event.ChatMessageEvent;
import com.pengcheng.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 聊天消息通知订阅器（Sprint B 收尾）
 *
 * 监听 ChatMessageEvent，给接收方推站内通知。
 * 与 ImConversationService 解耦：会话索引 vs 通知推送，各订阅各的。
 *
 * 业务规则：
 *   - 仅单聊（receiverId > 0）触发通知；群聊由 GroupMessage 走另一通道
 *   - 业务消息类型（CARD/LOCATION/GOODS/...）展示带类型标签的预览
 *   - 失败不向外抛，避免影响主流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageNotificationListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onChatMessage(ChatMessageEvent event) {
        if (event.getReceiverId() == null || event.getReceiverId() <= 0) {
            return;  // 群发或系统消息，跳过
        }
        try {
            Notification notice = buildNotification(event);
            notificationService.createNotification(notice);
        } catch (Exception e) {
            log.warn("[ChatMessageNotification] 推送失败 messageId={}: {}",
                    event.getMessageId(), e.getMessage());
        }
    }

    private Notification buildNotification(ChatMessageEvent event) {
        Notification n = new Notification();
        n.setUserId(event.getReceiverId());
        n.setType(1);  // 1 = 聊天消息类
        n.setBizType("chat");
        n.setBizId(event.getMessageId());
        n.setTitle("新消息");
        n.setContent(buildPreview(event));
        n.setReadStatus(0);
        n.setCreateTime(LocalDateTime.now());
        return n;
    }

    private String buildPreview(ChatMessageEvent event) {
        if (event.getBusinessType() != null && !event.getBusinessType().isBlank()) {
            return "[" + event.getBusinessType() + "] " + truncate(event.getContent(), 30);
        }
        Integer t = event.getMsgType();
        if (t != null) {
            return switch (t) {
                case 2 -> "[图片]";
                case 3 -> "[文件]";
                case 4 -> "[语音]";
                case 5 -> "[视频]";
                default -> truncate(event.getContent(), 80);
            };
        }
        return truncate(event.getContent(), 80);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
