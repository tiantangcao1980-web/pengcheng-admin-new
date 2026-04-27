-- OKR 模块 V62
-- OKR 周期（季度/年度）
CREATE TABLE okr_period (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(32) NOT NULL UNIQUE COMMENT 'e.g. 2026Q2',
    name        VARCHAR(64) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    status      TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1进行中 2已结束',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OKR 周期';

-- 目标 Objective
CREATE TABLE okr_objective (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    period_id   BIGINT NOT NULL,
    owner_id    BIGINT NOT NULL,
    owner_type  VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'USER/DEPT/COMPANY',
    parent_id   BIGINT COMMENT '上级目标，对齐用',
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    progress    INT NOT NULL DEFAULT 0 COMMENT '0-100，由 KR 平均自动算',
    weight      INT NOT NULL DEFAULT 100,
    status      TINYINT NOT NULL DEFAULT 0 COMMENT '0进行中 1完成 2取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_owner (owner_type, owner_id, period_id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OKR 目标';

-- 关键结果 Key Result
CREATE TABLE okr_key_result (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    objective_id  BIGINT NOT NULL,
    title         VARCHAR(255) NOT NULL,
    measure_type  VARCHAR(16) NOT NULL COMMENT 'NUMBER/PERCENT/MILESTONE/BOOLEAN',
    target_value  DECIMAL(15,2),
    current_value DECIMAL(15,2) DEFAULT 0,
    unit          VARCHAR(16),
    progress      INT NOT NULL DEFAULT 0,
    weight        INT NOT NULL DEFAULT 25,
    status        TINYINT NOT NULL DEFAULT 0,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_objective (objective_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OKR 关键结果';

-- 周/月度回顾 Check-in
CREATE TABLE okr_checkin (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    objective_id  BIGINT NOT NULL,
    key_result_id BIGINT,
    user_id       BIGINT NOT NULL,
    week_index    INT NOT NULL COMMENT '周次（1-52）',
    progress      INT NOT NULL,
    confidence    TINYINT NOT NULL DEFAULT 5 COMMENT '1-10 信心指数',
    summary       TEXT,
    issues        TEXT COMMENT '阻碍',
    next_steps    TEXT,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_objective_week (objective_id, week_index),
    KEY idx_user_week (user_id, week_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OKR 周期回顾 Check-in';
