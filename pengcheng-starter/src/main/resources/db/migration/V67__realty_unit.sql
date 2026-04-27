-- V67: 户型 + 房源 + 房源状态日志

-- 户型表
CREATE TABLE realty_house_type (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id    BIGINT NOT NULL COMMENT '楼盘 ID',
    code          VARCHAR(32) NOT NULL COMMENT '户型代码 A1/B2 等',
    name          VARCHAR(128) NOT NULL COMMENT '户型名称',
    bedrooms      TINYINT NOT NULL DEFAULT 0 COMMENT '卧室数量',
    living_rooms  TINYINT NOT NULL DEFAULT 0 COMMENT '客厅数量',
    bathrooms     TINYINT NOT NULL DEFAULT 0 COMMENT '卫生间数量',
    area          DECIMAL(10,2) NOT NULL COMMENT '建筑面积 m²',
    inside_area   DECIMAL(10,2) COMMENT '套内面积',
    orientation   VARCHAR(32) COMMENT '朝向：南/北/东/西/南北/...',
    layout_image  VARCHAR(512) COMMENT '户型图 OSS URL',
    base_price    DECIMAL(15,2) COMMENT '指导价',
    description   TEXT,
    enabled       TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-是 0-否',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_code (project_id, code),
    KEY idx_project (project_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='户型表';

-- 房源表（一栋楼一层一户的颗粒度）
CREATE TABLE realty_unit (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      BIGINT NOT NULL COMMENT '楼盘 ID',
    house_type_id   BIGINT NOT NULL COMMENT '户型 ID',
    building        VARCHAR(32) NOT NULL COMMENT '楼栋号 1/2/3 号楼',
    floor           SMALLINT NOT NULL COMMENT '楼层',
    unit_no         VARCHAR(32) NOT NULL COMMENT '房号 0301',
    full_no         VARCHAR(64) NOT NULL COMMENT '完整编号 1-3-0301',
    area            DECIMAL(10,2) NOT NULL COMMENT '建筑面积 m²',
    list_price      DECIMAL(15,2) NOT NULL COMMENT '挂牌价',
    actual_price    DECIMAL(15,2) COMMENT '实际成交价',
    status          VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/RESERVED/SUBSCRIBED/SIGNED/SOLD/UNAVAILABLE',
    locked_by       BIGINT COMMENT '锁定人（认购环节）',
    locked_until    DATETIME COMMENT '锁定到期时间',
    customer_id     BIGINT COMMENT '当前关联客户',
    deal_id         BIGINT COMMENT '关联成交单',
    remark          VARCHAR(512),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_full_no (project_id, full_no),
    KEY idx_project_status (project_id, status),
    KEY idx_house_type (house_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源表';

-- 房源状态变更日志（审计）
CREATE TABLE realty_unit_status_log (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    unit_id      BIGINT NOT NULL COMMENT '房源 ID',
    from_status  VARCHAR(16) COMMENT '变更前状态',
    to_status    VARCHAR(16) NOT NULL COMMENT '变更后状态',
    operator_id  BIGINT NOT NULL COMMENT '操作人',
    customer_id  BIGINT COMMENT '关联客户',
    deal_id      BIGINT COMMENT '关联成交单',
    reason       VARCHAR(255) COMMENT '变更原因',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_unit (unit_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源状态变更日志';
