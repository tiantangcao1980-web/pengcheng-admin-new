-- V63: 多维表格自动化（触发器 + 动作）
-- Phase 4 J2: smart_table_automation_rule + smart_table_automation_log

CREATE TABLE smart_table_automation_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_id        BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    enabled         TINYINT NOT NULL DEFAULT 1,
    trigger_type    VARCHAR(32) NOT NULL COMMENT 'RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/FIELD_CHANGED/SCHEDULED',
    trigger_config  TEXT COMMENT 'JSON {fieldKey?, when?: cron}',
    condition_json  TEXT COMMENT 'JSON DSL 条件表达式（可空表示无条件）',
    actions_json    TEXT NOT NULL COMMENT '[{type, params}, ...] type ∈ CREATE_TODO/SEND_EMAIL/UPDATE_RECORD/CALL_WEBHOOK/SEND_NOTIFICATION',
    create_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_table_enabled (table_id, enabled, trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维表格自动化规则';

CREATE TABLE smart_table_automation_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id       BIGINT NOT NULL,
    table_id      BIGINT NOT NULL,
    record_id     BIGINT,
    trigger_type  VARCHAR(32) NOT NULL,
    success       TINYINT NOT NULL,
    actions_count INT NOT NULL DEFAULT 0,
    error_msg     VARCHAR(1024),
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rule_time (rule_id, create_time),
    KEY idx_table (table_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维表格自动化执行日志';
