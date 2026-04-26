-- =====================================================
-- V47__customer_realty_ext.sql
-- V4.0 闭环③ - 房产专属字段下沉到 customer_realty_ext 扩展表
-- 关键设计：
--   1) 新建扩展表 customer_realty_ext，1:1 关联 customer.id
--   2) 把 customer 表中现有的房产专属字段数据平滑迁移到扩展表
--   3) 主表字段保留（NOT DROP），由 Service 双写一段时间，最终在下一个大版本统一摘除
-- =====================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新建扩展表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `customer_realty_ext` (
    `customer_id`             BIGINT      NOT NULL COMMENT '主键，等于 customer.id（1:1）',
    `visit_count`             INT         NULL DEFAULT NULL COMMENT '带看人数',
    `visit_time`              DATETIME    NULL DEFAULT NULL COMMENT '带看时间',
    `alliance_id`             BIGINT      NULL DEFAULT NULL COMMENT '带看公司（联盟商ID）',
    `agent_name`              VARCHAR(50) NULL DEFAULT NULL COMMENT '经纪人姓名',
    `agent_phone`             VARCHAR(20) NULL DEFAULT NULL COMMENT '经纪人联系方式',
    `deal_probability`        DECIMAL(5,4) NULL DEFAULT NULL COMMENT '成交概率（0-1）',
    `protection_expire_time`  DATETIME    NULL DEFAULT NULL COMMENT '保护期到期时间',
    `report_no`               VARCHAR(50) NULL DEFAULT NULL COMMENT '报备编号（房产业务）',
    `tenant_id`               BIGINT      NULL DEFAULT NULL,
    `create_time`             DATETIME    NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`customer_id`),
    KEY `idx_cre_alliance` (`alliance_id`),
    KEY `idx_cre_visit_time` (`visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户·房产行业扩展字段';

-- ----------------------------
-- 2. 平滑迁移 customer -> customer_realty_ext
--    使用 INSERT IGNORE 保证脚本可重入
-- ----------------------------
INSERT IGNORE INTO `customer_realty_ext`(
    `customer_id`, `visit_count`, `visit_time`, `alliance_id`, `agent_name`, `agent_phone`,
    `deal_probability`, `protection_expire_time`, `report_no`, `create_time`, `update_time`
)
SELECT
    c.`id`,
    c.`visit_count`,
    c.`visit_time`,
    c.`alliance_id`,
    c.`agent_name`,
    c.`agent_phone`,
    c.`deal_probability`,
    c.`protection_expire_time`,
    c.`report_no`,
    c.`create_time`,
    c.`update_time`
FROM `customer` c
WHERE c.`deleted` = 0;

-- ----------------------------
-- 3. 主表字段保留（NOT DROP）：
--    Service 层进入双写期，下一大版本（V5）才彻底摘除主表行业字段。
-- ----------------------------

-- =====================================================
-- 回滚（仅删除新表，不复原 customer 主表数据；主表字段未动）：
--   DROP TABLE IF EXISTS `customer_realty_ext`;
-- 完整下线（待 Service 切流完毕后，下一版本执行）：
--   ALTER TABLE `customer`
--       DROP COLUMN `visit_count`,
--       DROP COLUMN `visit_time`,
--       DROP COLUMN `alliance_id`,
--       DROP COLUMN `agent_name`,
--       DROP COLUMN `agent_phone`,
--       DROP COLUMN `deal_probability`,
--       DROP COLUMN `protection_expire_time`;
-- =====================================================
