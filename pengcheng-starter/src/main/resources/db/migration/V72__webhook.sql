-- =====================================================================
-- V72: 事件总线 + Webhook 订阅中心 + 投递重试（L3 — Phase 6）
-- =====================================================================

-- 事件类型注册表
CREATE TABLE webhook_event_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL COMMENT 'e.g. customer.created/deal.signed',
    name        VARCHAR(128) NOT NULL,
    category    VARCHAR(32)  NOT NULL COMMENT 'crm/oa/finance/...',
    payload_schema TEXT      COMMENT 'JSON Schema',
    enabled     TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook 事件类型';

INSERT INTO webhook_event_type (code, name, category) VALUES
('customer.created', '客户新增', 'crm'),
('customer.updated', '客户更新', 'crm'),
('lead.created', '线索新增', 'crm'),
('lead.assigned', '线索分配', 'crm'),
('lead.converted', '线索转客户', 'crm'),
('deal.signed', '成交签约', 'crm'),
('deal.cancelled', '成交取消', 'crm'),
('approval.created', '审批发起', 'oa'),
('approval.approved', '审批通过', 'oa'),
('approval.rejected', '审批驳回', 'oa'),
('receivable.recorded', '回款到账', 'finance'),
('receivable.overdue', '回款逾期', 'finance');

-- Webhook 订阅
CREATE TABLE webhook_subscription (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    name          VARCHAR(128) NOT NULL,
    url           VARCHAR(512) NOT NULL,
    event_codes   TEXT         NOT NULL COMMENT '逗号分隔订阅事件 code',
    secret        VARCHAR(128) NOT NULL COMMENT '签名密钥（receiver 验签用）',
    headers       TEXT         COMMENT 'JSON 自定义请求头',
    enabled       TINYINT      NOT NULL DEFAULT 1,
    last_delivery_at DATETIME,
    last_delivery_status VARCHAR(16),
    failure_count INT          NOT NULL DEFAULT 0,
    create_by     BIGINT       NOT NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook 订阅';

-- 投递记录 + 重试队列
CREATE TABLE webhook_delivery (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    subscription_id BIGINT       NOT NULL,
    tenant_id       BIGINT       NOT NULL,
    event_code      VARCHAR(64)  NOT NULL,
    event_id        VARCHAR(64)  NOT NULL COMMENT '事件唯一标识，receiver 幂等用',
    payload         LONGTEXT     NOT NULL,
    request_url     VARCHAR(512) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/DEAD',
    attempt_count   INT          NOT NULL DEFAULT 0,
    next_attempt_at DATETIME,
    response_status INT,
    response_body   TEXT,
    error_msg       VARCHAR(512),
    last_attempt_at DATETIME,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status_next (status, next_attempt_at),
    KEY idx_subscription (subscription_id, status, create_time),
    KEY idx_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook 投递记录';
