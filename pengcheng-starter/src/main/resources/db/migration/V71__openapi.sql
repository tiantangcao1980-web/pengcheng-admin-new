-- =====================================================================
-- V71: OpenAPI 第三方开发者平台（L2 — Phase 6 SaaS 生态）
-- =====================================================================

-- API Key 表（AK 公开 / SK 哈希存储 / scopes 限定能力 / rate_limit 限流）
CREATE TABLE openapi_key (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL COMMENT '所属租户',
    name            VARCHAR(128) NOT NULL COMMENT '密钥别名',
    access_key      VARCHAR(64)  NOT NULL COMMENT 'Access Key（公开）',
    secret_key_hash VARCHAR(128) NOT NULL COMMENT 'Secret Key SHA256 摘要（不可逆）',
    secret_preview  VARCHAR(16)  COMMENT '前 4 + ... + 后 4 用于 UI 展示',
    scopes          TEXT         COMMENT 'JSON 数组 ["customer:read","crm:write",...]',
    rate_limit      INT          NOT NULL DEFAULT 60 COMMENT '每分钟请求数上限',
    expires_at      DATETIME     COMMENT '过期时间，NULL 表示不过期',
    last_used_at    DATETIME     COMMENT '最近使用时间',
    last_used_ip    VARCHAR(64)  COMMENT '最近使用 IP',
    enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 1 启用',
    created_by      BIGINT       NOT NULL,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_access_key (access_key),
    KEY idx_tenant_enabled (tenant_id, enabled),
    KEY idx_access_key_enabled (access_key, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI 密钥';

-- API 调用日志（计量 + 审计）
CREATE TABLE openapi_call_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    access_key    VARCHAR(64)  NOT NULL COMMENT '调用方 AK',
    tenant_id     BIGINT       NOT NULL,
    method        VARCHAR(8)   NOT NULL,
    path          VARCHAR(255) NOT NULL,
    status_code   INT          NOT NULL,
    request_id    VARCHAR(64)  NOT NULL COMMENT '本次请求唯一标识',
    duration_ms   INT          NOT NULL DEFAULT 0,
    request_ip    VARCHAR(64),
    error_msg     VARCHAR(255),
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_id (request_id),
    KEY idx_ak_time (access_key, create_time),
    KEY idx_tenant_time (tenant_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI 调用日志';
