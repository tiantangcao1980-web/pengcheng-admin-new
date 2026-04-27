-- =====================================================================
-- V73: SaaS 套餐/订阅/账单/计量（L4 — Phase 6）
-- =====================================================================

CREATE TABLE saas_plan (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(32)  NOT NULL COMMENT 'free/pro/enterprise',
    name            VARCHAR(64)  NOT NULL,
    price_per_month DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_users       INT          NOT NULL DEFAULT 0 COMMENT '0 表示无限',
    max_storage_gb  INT          NOT NULL DEFAULT 0,
    max_api_calls_per_month INT  NOT NULL DEFAULT 0,
    features        TEXT         COMMENT 'JSON 数组：开放 plugin code',
    enabled         TINYINT      NOT NULL DEFAULT 1,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SaaS 套餐';

INSERT INTO saas_plan (code, name, price_per_month, max_users, max_storage_gb, max_api_calls_per_month, features) VALUES
('free', '免费版', 0, 5, 1, 1000, '["realty"]'),
('pro', '专业版', 299, 50, 50, 100000, '["realty","education","decoration"]'),
('enterprise', '企业版', 1999, 0, 500, 1000000, '["*"]');

CREATE TABLE tenant_subscription (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT   NOT NULL,
    plan_id      BIGINT   NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'TRIAL' COMMENT 'TRIAL/ACTIVE/EXPIRED/CANCELLED',
    start_date   DATE     NOT NULL,
    end_date     DATE     NOT NULL,
    auto_renew   TINYINT  NOT NULL DEFAULT 0,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id, status, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户订阅';

CREATE TABLE saas_bill (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    bill_no         VARCHAR(64)  NOT NULL,
    tenant_id       BIGINT       NOT NULL,
    subscription_id BIGINT       NOT NULL,
    period_start    DATE         NOT NULL,
    period_end      DATE         NOT NULL,
    base_amount     DECIMAL(10,2) NOT NULL,
    overage_amount  DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/OVERDUE/REFUNDED',
    paid_at         DATETIME,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no),
    KEY idx_tenant_status (tenant_id, status, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SaaS 账单';

CREATE TABLE saas_usage_metric (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id     BIGINT NOT NULL,
    metric_type   VARCHAR(32) NOT NULL COMMENT 'MAU/API_CALLS/STORAGE_GB',
    period_yyyymm VARCHAR(6)  NOT NULL,
    value_num     BIGINT  NOT NULL DEFAULT 0,
    last_update   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_metric_period (tenant_id, metric_type, period_yyyymm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SaaS 计量数据';
