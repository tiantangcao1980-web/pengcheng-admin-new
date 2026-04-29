package com.pengcheng.message.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 聊天消息领域事件
 *
 * 在 SysChatMessageService.send() 后发布，订阅方按需消费：
 *   - ConversationUpsertListener: 更新 im_conversation 行
 *   - 未来扩展：合规监控、AI 摘要、推送等
 */
@Getter
public class ChatMessageEvent extends ApplicationEvent {

    private final Long messageId;
    private final Long senderId;
    private final Long receiverId;
    private final String content;
    private final Integer msgType;
    /** 业务消息类型（任务 8 用），如 CARD/LOCATION/GOODS/FORM/CHOICE/EVENT */
    private final String businessType;

    public ChatMessageEvent(Object source, Long messageId, Long senderId,
                            Long receiverId, String content, Integer msgType,
                            String businessType) {
        super(source);
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.msgType = msgType;
        this.businessType = businessType;
    }
}
