-- V66: 行业插件框架（Phase 5 K1）
-- 插件注册表（启动同步：所有 IndustryPlugin Bean 同步到此表）
CREATE TABLE industry_plugin (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code         VARCHAR(64)  NOT NULL                COMMENT '插件唯一代码，e.g. realty/edu/decoration',
    name         VARCHAR(128) NOT NULL                COMMENT '插件显示名称',
    version      VARCHAR(32)  NOT NULL                COMMENT '插件版本号',
    description  VARCHAR(512)                         COMMENT '插件描述',
    vendor       VARCHAR(128) DEFAULT 'MasterLife'    COMMENT '插件提供方',
    icon         VARCHAR(64)                          COMMENT '图标标识（对应前端图标库 key）',
    enabled      TINYINT      NOT NULL DEFAULT 0      COMMENT '全局是否启用（0-禁用 1-启用，管理员手动开关）',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业插件注册表';

-- 租户级插件启用（覆盖全局开关，租户管理员自助开启）
CREATE TABLE tenant_plugin (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT      NOT NULL                COMMENT '租户ID',
    plugin_code VARCHAR(64) NOT NULL                COMMENT '插件代码，关联 industry_plugin.code',
    enabled     TINYINT     NOT NULL DEFAULT 1      COMMENT '是否启用（0-禁用 1-启用）',
    enabled_by  BIGINT                              COMMENT '操作人用户ID',
    enabled_at  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_plugin (tenant_id, plugin_code),
    KEY idx_plugin (plugin_code, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户级插件启用配置';

-- 预置房产行业插件元数据（enabled=0，需管理员或租户自助开启）
INSERT INTO industry_plugin (code, name, version, description, vendor, icon, enabled)
VALUES ('realty', '房产销售', '1.0.0',
        '楼盘/客户/带看/认购/签约/回款全链路，适用于新房销售场景',
        'MasterLife', 'BusinessOutline', 0);
