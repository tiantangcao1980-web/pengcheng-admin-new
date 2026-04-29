-- ----------------------------
-- V17: V1.0 周五上线收尾 — 6 大用户新需求字段补全 + 菜单补丁
-- 关联 doc/V4.0-LITE-ROADMAP.md 周五上线决策报告
-- 不破坏任何现有数据；所有 ALTER 都是 ADD COLUMN，无 DROP
-- ----------------------------

-- ========================================================
-- 需求 e: 客户管理 — 加性别字段
-- ========================================================
ALTER TABLE `customer`
  ADD COLUMN `gender` CHAR(1) NULL DEFAULT NULL COMMENT '性别：M-男 F-女 O-其他' AFTER `customerName`;

-- 性别字段加索引（非必须，但便于报表）
-- 仅在字段存在时加（防止重跑失败，使用条件）
-- 注：MySQL 8 才支持 CREATE INDEX IF NOT EXISTS，这里直接 try
ALTER TABLE `customer`
  ADD INDEX `idx_customer_gender` (`gender`) USING BTREE;

-- ========================================================
-- 需求 c: 拜访记录 — 用户类型/带看公司/带看日期+时间
-- ========================================================
ALTER TABLE `customer_visit`
  ADD COLUMN `visit_date` DATE NULL DEFAULT NULL COMMENT '带看日期（必填，从 actual_visit_time 抽取）' AFTER `actual_visit_time`,
  ADD COLUMN `visit_time_only` TIME NULL DEFAULT NULL COMMENT '带看时间（选填，可单独设置）' AFTER `visit_date`,
  ADD COLUMN `visit_company` VARCHAR(200) NULL DEFAULT NULL COMMENT '带看公司' AFTER `visit_time_only`,
  ADD COLUMN `user_type` TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型：1-联盟商 2-开发商' AFTER `visit_company`,
  ADD COLUMN `partner_id` BIGINT NULL DEFAULT NULL COMMENT '关联联盟商或开发商 ID（按 user_type 路由查询）' AFTER `user_type`,
  ADD INDEX `idx_visit_partner` (`user_type`, `partner_id`) USING BTREE;

-- 已有数据迁移：visit_date 从 actual_visit_time 抽取
UPDATE `customer_visit` SET `visit_date` = DATE(`actual_visit_time`) WHERE `visit_date` IS NULL AND `actual_visit_time` IS NOT NULL;

-- ========================================================
-- 需求 a: 成交佣金扩展
-- ========================================================
-- a-1: customer_deal 加 房号(已有 room_no) + 面积 + 总价 + 订单号
ALTER TABLE `customer_deal`
  ADD COLUMN `room_area` DECIMAL(10,2) NULL DEFAULT NULL COMMENT '成交面积（平米）' AFTER `room_no`,
  ADD COLUMN `total_price` DECIMAL(14,2) NULL DEFAULT NULL COMMENT '成交总价（含税）' AFTER `room_area`,
  ADD COLUMN `order_no` VARCHAR(64) NULL DEFAULT NULL COMMENT '成交订单号（业主签约号）' AFTER `total_price`,
  ADD INDEX `idx_deal_order_no` (`order_no`) USING BTREE;

-- a-2: commission 加 物业类型 + 客户籍贯
ALTER TABLE `commission`
  ADD COLUMN `property_type` VARCHAR(32) NULL DEFAULT NULL COMMENT '物业类型：RESIDENTIAL住宅 COMMERCIAL商铺 APARTMENT公寓 OFFICE写字楼 OTHER其他' AFTER `platform_fee`,
  ADD COLUMN `customer_origin` VARCHAR(16) NULL DEFAULT 'DOMESTIC' COMMENT '客户籍贯：DOMESTIC内地 OVERSEAS境外' AFTER `property_type`,
  ADD INDEX `idx_commission_property` (`property_type`, `customer_origin`) USING BTREE;

-- a-3: commission_detail 加 4 种角色提成
ALTER TABLE `commission_detail`
  ADD COLUMN `dealer_reward` DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT '下游经销商奖励' AFTER `platform_reward`,
  ADD COLUMN `site_person_reward` DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT '驻场人员提成' AFTER `dealer_reward`,
  ADD COLUMN `channel_specialist_reward` DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT '渠道专员提成' AFTER `site_person_reward`,
  ADD COLUMN `channel_manager_reward` DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT '渠道经理提成' AFTER `channel_specialist_reward`;

-- ========================================================
-- 需求 b: 楼盘扩展 — 保护期 / 交楼时间 / 交付标准 / 赠品
-- ========================================================
ALTER TABLE `project`
  ADD COLUMN `protect_period_days` INT NULL DEFAULT 15 COMMENT '到访保护期（天，默认 15；常用 7/15/30）' AFTER `description`,
  ADD COLUMN `delivery_date` DATE NULL DEFAULT NULL COMMENT '交楼时间' AFTER `protect_period_days`,
  ADD COLUMN `delivery_standard` VARCHAR(500) NULL DEFAULT NULL COMMENT '交付标准（毛坯/精装/拎包入住等描述）' AFTER `delivery_date`,
  ADD COLUMN `gift_items` VARCHAR(500) NULL DEFAULT NULL COMMENT '赠送物品（家电/家具/物业费等）' AFTER `delivery_standard`;

-- b-2: project_commission_rule 增加按物业类型 + 客户籍贯双维度配置
-- 同一楼盘可以有多条规则（住宅+内地一条，住宅+境外另一条，商铺+内地另一条…）
ALTER TABLE `project_commission_rule`
  ADD COLUMN `property_type` VARCHAR(32) NULL DEFAULT 'RESIDENTIAL' COMMENT '物业类型，与 commission.property_type 取值对齐' AFTER `project_id`,
  ADD COLUMN `customer_origin` VARCHAR(16) NULL DEFAULT 'DOMESTIC' COMMENT '客户籍贯，与 commission.customer_origin 取值对齐' AFTER `property_type`,
  ADD INDEX `idx_rule_dim` (`project_id`, `property_type`, `customer_origin`) USING BTREE;

-- ========================================================
-- 需求 d: 小程序新增 物料申请 + 用车申请 菜单（页面前端补）
-- ========================================================
INSERT INTO `sys_menu` (id, parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted) VALUES
  -- 协作办公(311)目录下新增
  (340, 311, '物料申请', 2, '/apply/material', 'apply/MaterialApply', 'sys:apply:material', 'CubeOutline', 10, 1, 1, 0, 0),
  (341, 311, '用车申请', 2, '/apply/car',      'apply/CarApply',      'sys:apply:car',      'CarOutline',  11, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE name=VALUES(name), permission=VALUES(permission);

-- 给超管角色（id=1）授权
INSERT INTO `sys_role_menu` (role_id, menu_id) VALUES (1, 340), (1, 341)
ON DUPLICATE KEY UPDATE role_id=role_id;

-- ========================================================
-- 需求 e 补充: 客户报备状态枚举对齐前端
-- 现 customer.status 枚举：
--   1-已报备 2-已到访 3-已成交 4-已流失 5-已退订
-- 不改 DDL（已是 TINYINT），仅在文档约定
-- ========================================================
