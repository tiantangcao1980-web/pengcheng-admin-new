-- =============================================================
-- V44__ai_copilot_action_log.sql
-- V4.0 MVP 闭环④ AI 智能助手 - Copilot 操作日志表
-- 任务规划编号 V21（在 V43 之后顺延，本表为本任务第 2 张表）
-- 记录用户在 Copilot 对话中触发的业务动作（新建跟进/创建待办/提交审批等）
-- =============================================================

CREATE TABLE IF NOT EXISTS `ai_copilot_action_log` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`       BIGINT        NULL DEFAULT NULL COMMENT '租户ID',
    `conversation_id` VARCHAR(64)   NULL DEFAULT NULL COMMENT '会话ID（关联 ai_memory）',
    `user_id`         BIGINT        NOT NULL COMMENT '触发用户ID',
    `page_path`       VARCHAR(255)  NULL DEFAULT NULL COMMENT '触发页面 path（前端注入）',
    `action_code`     VARCHAR(64)   NOT NULL COMMENT '动作编码：FOLLOW_UP_CREATE / TODO_CREATE / APPROVAL_SUBMIT',
    `payload`         TEXT          NULL DEFAULT NULL COMMENT '动作参数（JSON）',
    `confirm_token`   VARCHAR(64)   NULL DEFAULT NULL COMMENT '前端二次确认 token（防误触）',
    `status`          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/EXECUTED/CANCELLED/FAILED',
    `result_summary`  VARCHAR(500)  NULL DEFAULT NULL COMMENT '执行结果摘要',
    `error_message`   VARCHAR(500)  NULL DEFAULT NULL COMMENT '失败原因',
    `executed_at`     DATETIME      NULL DEFAULT NULL COMMENT '执行时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_action_time` (`user_id`, `action_code`, `create_time`),
    KEY `idx_conversation`     (`conversation_id`),
    KEY `idx_confirm_token`    (`confirm_token`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Copilot 对话动作操作日志（V4.0 MVP 闭环④）';
