package com.pengcheng.push.unified;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推送通道审计日志实体（对应 V55 / push_channel_log 表）。
 *
 * <p>字段包含 D5 V55 的列（user_id/channel/biz_type/biz_id/title/success/reason/...）
 * 以及 E5 JpushUnifiedSender 增量需要的别名（target/auditStatus/failReason — 通过 @TableField 复用列）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("push_channel_log")
public class PushChannelLog implements Serializable {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收用户 ID（V55 列 user_id；E5 用 String "target" 别名） */
    private Long userId;

    /** target 别名 — 与 userId 同列（V55 user_id），E5 sender 写 String 类型， */
    @TableField(exist = false)
    private String target;

    private String channel;

    private String bizType;
    private Long bizId;
    private String title;

    /** D5 success 0/1；E5 用 STATUS_SUCCESS/STATUS_FAIL 字符串别名 → 通过 auditStatus → success 的转换 setter 实现 */
    private Integer success;

    /** E5 别名：FAIL/SUCCESS — 不映射到 DB 列，写入时由 setter 同步到 success */
    @TableField(exist = false)
    private String auditStatus;

    /** D5 reason；E5 用 failReason 别名 */
    private String reason;

    @TableField(exist = false)
    private String failReason;

    private String subscribeTemplateId;
    private LocalDateTime createTime;

    /** Builder：auditStatus → success */
    public static class PushChannelLogBuilder {
        public PushChannelLogBuilder auditStatus(String status) {
            this.auditStatus = status;
            this.success = STATUS_SUCCESS.equals(status) ? 1 : 0;
            return this;
        }
        public PushChannelLogBuilder failReason(String reason) {
            this.failReason = reason;
            this.reason = reason;
            return this;
        }
        public PushChannelLogBuilder target(String target) {
            this.target = target;
            try {
                this.userId = Long.parseLong(target);
            } catch (Exception ignored) {
                // 非数字 userId（小程序 openId 等）— 留空 userId，target 字段不入库
            }
            return this;
        }
    }
}
