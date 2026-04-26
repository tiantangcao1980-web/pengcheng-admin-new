package com.pengcheng.ai.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能提醒调度规则（V4.0 MVP 闭环④）
 * <p>
 * 表 ai_reminder_rule，三种 rule_type：
 * <ul>
 *     <li>DAILY      —— 按 cron_expr 每日定时（如 0 0 9 * * ? 每日 9:00）</li>
 *     <li>THRESHOLD  —— 按阈值（threshold_min 分钟）扫描堆积项（如审批堆积 2h）</li>
 *     <li>PRE_EXPIRE —— 按提前天数（pre_days）预警将到期项（如公海回收前 1 天）</li>
 * </ul>
 */
@Data
@TableName("ai_reminder_rule")
public class AiReminderRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    private String cronExpr;

    private Integer thresholdMin;

    private Integer preDays;

    private String targetScope;

    private String channel;

    private String template;

    private Integer enabled;

    private LocalDateTime lastFiredAt;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public boolean isEnabled() {
        return enabled != null && enabled == 1;
    }

    /** 规则类型常量（避免到处硬编码字符串） */
    public static final class Type {
        public static final String DAILY = "DAILY";
        public static final String THRESHOLD = "THRESHOLD";
        public static final String PRE_EXPIRE = "PRE_EXPIRE";

        private Type() {
        }
    }

    /** MVP 三个内置规则编码 */
    public static final class Code {
        public static final String DAILY_FOLLOWUP = "DAILY_FOLLOWUP";
        public static final String APPROVAL_PENDING = "APPROVAL_PENDING";
        public static final String POOL_RECYCLE_PRE = "POOL_RECYCLE_PRE";

        private Code() {
        }
    }
}
