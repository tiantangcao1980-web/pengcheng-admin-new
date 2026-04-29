-- =====================================================================
-- V23: 小程序订阅消息模板（V4.0 闭环⑤ 移动办公 - D5 任务）
-- =====================================================================
-- 把业务事件（审批/客户等）映射到具体的微信小程序订阅消息模板，
-- 不同微信主体下复用同一段业务代码。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `subscribe_msg_template` (
    `id`                  BIGINT NOT NULL COMMENT '模板 ID',
    `biz_type`            VARCHAR(64) NOT NULL COMMENT '业务类型 customer/approval/...',
    `event_code`          VARCHAR(64) NOT NULL COMMENT '业务事件 created/passed/rejected/...',
    `template_id`         VARCHAR(64) NOT NULL COMMENT '微信小程序模板 ID',
    `field_mapping_json`  TEXT DEFAULT NULL COMMENT '业务字段 -> 模板字段映射 JSON',
    `default_page`        VARCHAR(255) DEFAULT NULL COMMENT '默认跳转页面',
    `enabled`             TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0 否 1 是',
    `remark`              VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_event` (`biz_type`, `event_code`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序订阅消息模板';

-- 预置常用业务事件占位（templateId 留空，由用户在公众平台申请后填入）
INSERT IGNORE INTO `subscribe_msg_template`
    (`id`, `biz_type`, `event_code`, `template_id`, `field_mapping_json`, `default_page`, `enabled`, `remark`)
VALUES
    (1, 'approval', 'created',  '', '{"applicantName":"thing1","bizType":"thing2","time":"date3"}', '/pages/approval/detail', 0, '审批新到达 - 待用户填模板 ID'),
    (2, 'approval', 'passed',   '', '{"applicantName":"thing1","status":"phrase2","time":"date3"}', '/pages/approval/detail', 0, '审批通过'),
    (3, 'approval', 'rejected', '', '{"applicantName":"thing1","status":"phrase2","reason":"thing4"}', '/pages/approval/detail', 0, '审批驳回'),
    (4, 'customer', 'reminder', '', '{"customerName":"thing1","status":"phrase2","time":"date3"}', '/pages/customer/detail', 0, '客户跟进提醒');
