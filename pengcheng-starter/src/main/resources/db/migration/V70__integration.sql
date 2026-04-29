-- V70 IM 集成表（Phase 6）
-- IM 集成租户配置（按 tenant + provider 一份）
CREATE TABLE integration_provider_config (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT NOT NULL,
    provider      VARCHAR(32) NOT NULL COMMENT 'wecom/dingtalk/feishu',
    corp_id       VARCHAR(64) NOT NULL,
    agent_id      VARCHAR(64),
    secret_ref    VARCHAR(128) NOT NULL COMMENT 'sys_secret_vault 引用键，不直接存 secret',
    sync_enabled  TINYINT NOT NULL DEFAULT 1,
    last_sync_time DATETIME,
    last_sync_status VARCHAR(32),
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_provider (tenant_id, provider)
);

-- 用户在 IM 系统的映射（一个用户可同时绑定多个 provider）
CREATE TABLE integration_user_mapping (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    provider      VARCHAR(32) NOT NULL,
    external_id   VARCHAR(128) NOT NULL COMMENT 'wecom userId / dingtalk userId / feishu open_id',
    external_dept_ids TEXT COMMENT 'JSON [externalDeptId]',
    avatar        VARCHAR(512),
    bind_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_provider (user_id, provider),
    KEY idx_external (provider, external_id)
);

-- 部门映射
CREATE TABLE integration_dept_mapping (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT NOT NULL,
    dept_id       BIGINT NOT NULL,
    provider      VARCHAR(32) NOT NULL,
    external_id   VARCHAR(128) NOT NULL,
    external_parent_id VARCHAR(128),
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_provider (dept_id, provider),
    KEY idx_external (provider, external_id)
);

-- 同步任务日志
CREATE TABLE integration_sync_log (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id    BIGINT NOT NULL,
    provider     VARCHAR(32) NOT NULL,
    sync_type    VARCHAR(32) NOT NULL COMMENT 'CONTACT/MESSAGE/APPROVAL',
    success      TINYINT NOT NULL,
    affected     INT NOT NULL DEFAULT 0,
    error_msg    VARCHAR(1024),
    duration_ms  INT,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tenant_time (tenant_id, create_time)
);
