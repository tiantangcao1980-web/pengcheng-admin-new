-- =============================================================
-- V43__ai_reminder_rule.sql
-- V4.0 MVP 闭环④ AI 智能助手 - 智能提醒调度规则表
-- 任务规划编号 V20（在已有 V42 之后顺延，本表为本任务第 1 张表）
-- =============================================================

CREATE TABLE IF NOT EXISTS `ai_reminder_rule` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      BIGINT       NULL DEFAULT NULL COMMENT '租户ID（多租户隔离，可空）',
    `rule_code`      VARCHAR(64)  NOT NULL COMMENT '规则编码：DAILY_FOLLOWUP / APPROVAL_PENDING / POOL_RECYCLE_PRE',
    `rule_name`      VARCHAR(128) NOT NULL COMMENT '规则名称',
    `rule_type`      VARCHAR(32)  NOT NULL COMMENT '规则类型：DAILY/THRESHOLD/PRE_EXPIRE',
    `cron_expr`      VARCHAR(64)  NULL DEFAULT NULL COMMENT 'CRON 表达式（DAILY 类型必填）',
    `threshold_min`  INT          NULL DEFAULT NULL COMMENT '阈值（分钟，THRESHOLD 类型用）',
    `pre_days`       INT          NULL DEFAULT NULL COMMENT '提前天数（PRE_EXPIRE 类型用）',
    `target_scope`   VARCHAR(32)  NOT NULL DEFAULT 'OWNER' COMMENT '推送目标：OWNER/APPROVER/SUPERIOR/CUSTOM',
    `channel`        VARCHAR(64)  NOT NULL DEFAULT 'INTERNAL,APP' COMMENT '推送渠道：INTERNAL,APP,DINGTALK,FEISHU,WECOM 多个逗号分隔',
    `template`       VARCHAR(512) NULL DEFAULT NULL COMMENT '消息模板（占位符 ${name}/${count} 等）',
    `enabled`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
    `last_fired_at`  DATETIME     NULL DEFAULT NULL COMMENT '上一次触发时间',
    `create_by`      BIGINT       NULL DEFAULT NULL COMMENT '创建人ID',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `rule_code`),
    KEY `idx_enabled_type` (`enabled`, `rule_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 智能提醒调度规则表（V4.0 MVP 闭环④）';

-- 内置三条 MVP 规则：每日 9:00 待跟进；审批堆积 2h；公海回收前 1 天
INSERT INTO `ai_reminder_rule`
    (`rule_code`, `rule_name`, `rule_type`, `cron_expr`, `threshold_min`, `pre_days`, `target_scope`, `channel`, `template`, `enabled`)
VALUES
    ('DAILY_FOLLOWUP',     '每日待跟进客户提醒', 'DAILY',      '0 0 9 * * ?', NULL,    NULL, 'OWNER',    'INTERNAL,APP', '您今日有 ${count} 个客户待跟进，请及时处理。', 1),
    ('APPROVAL_PENDING',   '审批堆积提醒',         'THRESHOLD',  NULL,            120,  NULL, 'APPROVER', 'INTERNAL,APP', '您有 ${count} 个审批已等待超过 ${threshold} 分钟。',         1),
    ('POOL_RECYCLE_PRE',   '公海回收前提醒',       'PRE_EXPIRE', NULL,            NULL, 1,    'OWNER',    'INTERNAL,APP', '您名下 ${count} 个客户将在 1 天后回收到公海，请及时跟进。', 1)
ON DUPLICATE KEY UPDATE `rule_name` = VALUES(`rule_name`);
