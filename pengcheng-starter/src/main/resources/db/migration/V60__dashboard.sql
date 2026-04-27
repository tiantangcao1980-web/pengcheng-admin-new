-- V60: 看板卡片框架（Phase 3 数据决策闭环）

-- 卡片注册表（启用列表，运行时由 DashboardCardRegistry 同步 enable 状态）
CREATE TABLE dashboard_card_def (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(64)  NOT NULL UNIQUE COMMENT '卡片代码，对齐 SPI code()',
    name            VARCHAR(128) NOT NULL,
    category        VARCHAR(32)  NOT NULL,
    suggested_chart VARCHAR(32),
    default_cols    TINYINT      NOT NULL DEFAULT 4,
    default_rows    TINYINT      NOT NULL DEFAULT 3,
    description     VARCHAR(255),
    enabled         TINYINT      NOT NULL DEFAULT 1,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_category_enabled (category, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看板卡片定义表';

-- 用户布局（每用户/角色一份，layout_json 是 JSON 含 [{cardCode, x, y, w, h}, ...]）
CREATE TABLE dashboard_layout (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    owner_type   VARCHAR(16)  NOT NULL COMMENT 'USER/ROLE',
    owner_id     BIGINT       NOT NULL,
    name         VARCHAR(64)  NOT NULL DEFAULT '默认',
    layout_json  TEXT         NOT NULL,
    is_default   TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_owner_default (owner_type, owner_id, is_default),
    KEY idx_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看板布局表';
