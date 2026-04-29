package com.pengcheng.message.channel;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推送通道下发日志
 *
 * <p>对应 SQL Migration V22 的 push_channel_log 表，
 * 用于追踪三通道决策与下发结果，便于后续审计和告警。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("push_channel_log")
public class PushChannelLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收用户 ID */
    private Long userId;

    /** 决策选中的通道 code（appPush / mpSubscribe / webInbox / none） */
    private String channel;

    /** 业务类型 */
    private String bizType;

    /** 业务 ID */
    private Long bizId;

    /** 标题（脱敏后） */
    private String title;

    /** 是否成功：0 失败 1 成功 */
    private Integer success;

    /** 失败原因或降级链 */
    private String reason;

    /** 关联的订阅消息模板 ID（非订阅通道为空） */
    private String subscribeTemplateId;

    private LocalDateTime createTime;
}
