-- =====================================================================
-- V22: 推送通道下发日志（V4.0 闭环⑤ 移动办公 - D5 任务）
-- =====================================================================
-- 用于追踪三通道（APP 推送 / 小程序订阅消息 / Web 站内信）决策与下发结果。
-- 由 ChannelPushService 在每次 push 后写入；不影响业务事务。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `push_channel_log` (
    `id`                    BIGINT NOT NULL COMMENT '日志 ID',
    `user_id`               BIGINT NOT NULL COMMENT '接收用户 ID',
    `channel`               VARCHAR(32) NOT NULL COMMENT '通道：appPush/mpSubscribe/webInbox/none',
    `biz_type`              VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    `biz_id`                BIGINT DEFAULT NULL COMMENT '业务 ID',
    `title`                 VARCHAR(255) DEFAULT NULL COMMENT '标题（脱敏后）',
    `success`               TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功 0 否 1 是',
    `reason`                VARCHAR(512) DEFAULT NULL COMMENT '失败原因或降级链',
    `subscribe_template_id` VARCHAR(64) DEFAULT NULL COMMENT '关联订阅消息模板 ID',
    `create_time`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_channel_success_time` (`channel`, `success`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送通道下发日志';
