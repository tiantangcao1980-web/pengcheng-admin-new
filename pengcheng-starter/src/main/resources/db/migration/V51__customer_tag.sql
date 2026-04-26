-- =====================================================
-- V46__customer_tag.sql
-- V4.0 闭环③ - 客户标签
-- =====================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 客户标签 customer_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `customer_tag` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `tag_name`     VARCHAR(64)  NOT NULL,
    `color`        VARCHAR(20)  NULL DEFAULT '#1677FF' COMMENT '展示颜色（hex）',
    `category`     VARCHAR(40)  NULL DEFAULT NULL COMMENT '分类（意向/性格/渠道...)',
    `description`  VARCHAR(200) NULL DEFAULT NULL,
    `sort_order`   INT          NOT NULL DEFAULT 100,
    `enabled`      TINYINT      NOT NULL DEFAULT 1,
    `tenant_id`    BIGINT       NULL DEFAULT NULL,
    `create_by`    BIGINT       NULL DEFAULT NULL,
    `update_by`    BIGINT       NULL DEFAULT NULL,
    `create_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_tag_name` (`tag_name`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户标签';

-- ----------------------------
-- 2. 客户-标签关联 customer_tag_rel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `customer_tag_rel` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT,
    `customer_id`  BIGINT   NOT NULL,
    `tag_id`       BIGINT   NOT NULL,
    `tenant_id`    BIGINT   NULL DEFAULT NULL,
    `create_by`    BIGINT   NULL DEFAULT NULL,
    `create_time`  DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_tag_rel` (`customer_id`, `tag_id`),
    KEY `idx_ctr_customer` (`customer_id`),
    KEY `idx_ctr_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户-标签关系';

-- =====================================================
-- 回滚：
--   DROP TABLE IF EXISTS `customer_tag_rel`;
--   DROP TABLE IF EXISTS `customer_tag`;
-- =====================================================
