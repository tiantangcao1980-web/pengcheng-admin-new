-- =====================================================
-- V44__crm_custom_field.sql
-- V4.0 闭环③ - 自定义字段（EAV 模型）
-- 适用实体：lead / customer 等（用 entity_type 区分）
-- =====================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 自定义字段定义表 custom_field_def
-- ----------------------------
CREATE TABLE IF NOT EXISTS `custom_field_def` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字段ID',
    `entity_type`     VARCHAR(40)  NOT NULL COMMENT '实体类型：lead / customer / opportunity ...',
    `field_key`       VARCHAR(64)  NOT NULL COMMENT '字段 key（同实体内唯一，英文）',
    `label`           VARCHAR(120) NOT NULL COMMENT '显示名',
    `field_type`      VARCHAR(20)  NOT NULL COMMENT '类型：text/number/date/select/multi_select/file',
    `required`        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填',
    `default_value`   VARCHAR(500) NULL DEFAULT NULL COMMENT '默认值',
    `options_json`    JSON         NULL COMMENT 'select/multi_select 的选项 [{value,label}]',
    `validation_json` JSON         NULL COMMENT '校验规则：min/max/pattern/maxLength 等',
    `sort_order`      INT          NOT NULL DEFAULT 100,
    `enabled`         TINYINT      NOT NULL DEFAULT 1,
    `tenant_id`       BIGINT       NULL DEFAULT NULL,
    `create_by`       BIGINT       NULL DEFAULT NULL,
    `update_by`       BIGINT       NULL DEFAULT NULL,
    `create_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cfd_entity_key` (`entity_type`, `field_key`, `tenant_id`),
    KEY `idx_cfd_entity` (`entity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自定义字段定义表';

-- ----------------------------
-- 2. 自定义字段值表 custom_field_value
-- ----------------------------
CREATE TABLE IF NOT EXISTS `custom_field_value` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `entity_type`     VARCHAR(40)  NOT NULL,
    `entity_id`       BIGINT       NOT NULL COMMENT '关联业务实体的主键',
    `field_id`        BIGINT       NOT NULL COMMENT '关联 custom_field_def.id',
    `field_key`       VARCHAR(64)  NOT NULL COMMENT '冗余 field_key 加速查询',
    `value_text`      TEXT         NULL COMMENT '文本/选项值',
    `value_number`    DECIMAL(20,6) NULL DEFAULT NULL,
    `value_date`      DATETIME     NULL DEFAULT NULL,
    `value_json`      JSON         NULL COMMENT '多选/文件等复杂结构',
    `tenant_id`       BIGINT       NULL DEFAULT NULL,
    `create_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cfv_entity_field` (`entity_type`, `entity_id`, `field_id`),
    KEY `idx_cfv_entity` (`entity_type`, `entity_id`),
    KEY `idx_cfv_field` (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自定义字段值表（EAV）';

-- =====================================================
-- 性能建议（PRD §8.2 风险表已注明）：
--   - 频繁筛选/排序的字段应冗余到主业务表（热字段冗余）；
--   - 过滤复杂多条件查询时建议建复合查询表 + 离线同步；
--   - 单实体字段数 > 50 个时考虑列式或 JSON 列承载。
-- 回滚：
--   DROP TABLE IF EXISTS `custom_field_value`;
--   DROP TABLE IF EXISTS `custom_field_def`;
-- =====================================================
