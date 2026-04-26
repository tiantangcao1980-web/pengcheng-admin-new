package com.pengcheng.ai.reminder;

import lombok.Builder;
import lombok.Data;

/**
 * 提醒推送目标。
 * <p>
 * D5 推送渠道服务尚未到位时，本 DTO 通过 {@link ReminderPushPort} 由现有
 * {@code ChannelPushService} 兜底（钉钉/飞书/企业微信群聊），或仅落库内部消息。
 */
@Data
@Builder
public class ReminderTarget {

    /** 业务用户 ID（OWNER/APPROVER 等） */
    private Long userId;

    /** 关联记录数（模板占位 ${count}） */
    private Integer count;

    /** 用户名 / 角色名（模板占位 ${name}） */
    private String name;

    /** 渲染后的标题 */
    private String title;

    /** 渲染后的正文 */
    private String content;

    /** 来源规则编码（用于日志） */
    private String ruleCode;

    /** 业务关联记录主键列表（如客户ID/审批ID，逗号分隔，便于推送侧追溯） */
    private String relatedIds;
}
