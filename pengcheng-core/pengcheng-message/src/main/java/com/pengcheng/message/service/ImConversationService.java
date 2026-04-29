package com.pengcheng.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.message.entity.ImConversation;
import com.pengcheng.message.event.ChatMessageEvent;
import com.pengcheng.message.mapper.ImConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IM 会话索引服务
 *
 * 监听 ChatMessageEvent，自动维护 im_conversation 表（双行：发送者 + 接收者）。
 * 这样查询"最近联系人列表"无需扫消息表，O(N) → O(1)。
 *
 * 注意：未读数仍以 Redis 为权威（高频原子操作），DB 兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImConversationService {

    private final ImConversationMapper conversationMapper;

    /** 单聊会话ID 规则：min(uid,uid)_max(uid,uid)，保证两端落到同一会话 */
    public static String makeConversationId(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + "_" + max;
    }

    /**
     * 接收消息事件，upsert 双方会话行
     */
    @Async
    @EventListener
    public void onChatMessage(ChatMessageEvent event) {
        try {
            String convId = makeConversationId(event.getSenderId(), event.getReceiverId());
            // 发送者侧
            upsert(event.getSenderId(), event.getReceiverId(), convId, event, false);
            // 接收者侧（未读 +1）
            upsert(event.getReceiverId(), event.getSenderId(), convId, event, true);
        } catch (Exception e) {
            log.warn("[ImConversation] 处理 ChatMessageEvent 失败 messageId={}: {}",
                    event.getMessageId(), e.getMessage());
        }
    }

    /**
     * upsert 一行会话索引
     */
    private void upsert(Long ownerId, Long peerId, String convId,
                        ChatMessageEvent event, boolean increaseUnread) {
        ImConversation existing = conversationMapper.selectOne(
                new LambdaQueryWrapper<ImConversation>()
                        .eq(ImConversation::getOwnerId, ownerId)
                        .eq(ImConversation::getConversationId, convId));

        String preview = previewContent(event.getContent(), event.getMsgType(), event.getBusinessType());
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            ImConversation row = ImConversation.builder()
                    .conversationId(convId)
                    .ownerId(ownerId)
                    .peerId(peerId)
                    .peerType(ImConversation.PEER_SINGLE)
                    .lastMsgId(event.getMessageId())
                    .lastMsgContent(preview)
                    .lastMsgTime(now)
                    .unreadCount(increaseUnread ? 1 : 0)
                    .pinned(0).muted(0)
                    .build();
            conversationMapper.insert(row);
        } else {
            existing.setLastMsgId(event.getMessageId());
            existing.setLastMsgContent(preview);
            existing.setLastMsgTime(now);
            if (increaseUnread) {
                existing.setUnreadCount((existing.getUnreadCount() == null ? 0 : existing.getUnreadCount()) + 1);
            }
            conversationMapper.updateById(existing);
        }
    }

    /** 列出某用户的会话（按最近消息时间倒序） */
    public List<ImConversation> listByOwner(Long ownerId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getOwnerId, ownerId)
                .orderByDesc(ImConversation::getPinned)
                .orderByDesc(ImConversation::getLastMsgTime));
    }

    /** 标记会话已读（清零未读数） */
    public void clearUnread(Long ownerId, String conversationId) {
        ImConversation existing = conversationMapper.selectOne(
                new LambdaQueryWrapper<ImConversation>()
                        .eq(ImConversation::getOwnerId, ownerId)
                        .eq(ImConversation::getConversationId, conversationId));
        if (existing != null && existing.getUnreadCount() != null && existing.getUnreadCount() > 0) {
            existing.setUnreadCount(0);
            conversationMapper.updateById(existing);
        }
    }

    /** 消息预览（截断 + 业务消息标签） */
    private String previewContent(String content, Integer msgType, String businessType) {
        if (businessType != null && !businessType.isBlank()) {
            return "[" + businessType + "]";
        }
        if (msgType != null) {
            return switch (msgType) {
                case 2 -> "[图片]";
                case 3 -> "[文件]";
                case 4 -> "[语音]";
                case 5 -> "[视频]";
                default -> truncate(content, 100);
            };
        }
        return truncate(content, 100);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
