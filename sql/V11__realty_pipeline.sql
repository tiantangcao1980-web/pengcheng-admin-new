-- ----------------------------
-- V11: 销售漏斗 + 商机阶段
-- 关联 V1.0 Sprint A 第三任务：销售漏斗 + 商机阶段
-- ----------------------------

-- 1. 漏斗阶段定义表（管理员可后续维护）
CREATE TABLE IF NOT EXISTS `realty_pipeline_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段ID',
  `name` VARCHAR(64) NOT NULL COMMENT '阶段名称',
  `code` VARCHAR(32) NOT NULL COMMENT '阶段代码 (LEAD/INTENT/VISIT/SUBSCRIBE/SIGNED/LOST)',
  `order_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `win_rate` INT NOT NULL DEFAULT 0 COMMENT '默认胜率 0-100',
  `color` VARCHAR(16) NULL DEFAULT NULL COMMENT '看板列色值 #RRGGBB',
  `is_terminal` TINYINT NOT NULL DEFAULT 0 COMMENT '是否终态：0-否 1-是（签约/流失）',
  `active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pipeline_code` (`code`) USING BTREE,
  INDEX `idx_pipeline_order` (`order_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售漏斗阶段定义' ROW_FORMAT = DYNAMIC;

-- 默认 6 个阶段（房产销售标准流程）
INSERT INTO `realty_pipeline_stage` (`name`, `code`, `order_no`, `win_rate`, `color`, `is_terminal`) VALUES
  ('留资',   'LEAD',      1, 10,  '#909399', 0),
  ('意向',   'INTENT',    2, 25,  '#E6A23C', 0),
  ('看房',   'VISIT',     3, 45,  '#67C23A', 0),
  ('认筹',   'SUBSCRIBE', 4, 70,  '#409EFF', 0),
  ('签约',   'SIGNED',    5, 100, '#67C23A', 1),
  ('流失',   'LOST',      6, 0,   '#F56C6C', 1);

-- 2. 商机表（一个客户在某楼盘上的"销售机会"）
CREATE TABLE IF NOT EXISTS `realty_opportunity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商机ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `project_id` BIGINT NOT NULL COMMENT '楼盘项目ID',
  `stage_id` BIGINT NOT NULL COMMENT '当前阶段ID',
  `title` VARCHAR(200) NULL DEFAULT NULL COMMENT '商机标题，默认拼客户名+楼盘名',
  `expected_amount` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '预期成交金额',
  `expected_close_date` DATE NULL DEFAULT NULL COMMENT '预期关闭日期',
  `owner_id` BIGINT NULL DEFAULT NULL COMMENT '当前负责人（业务员）',
  `next_action` VARCHAR(500) NULL DEFAULT NULL COMMENT '下一步动作',
  `next_action_at` DATETIME NULL DEFAULT NULL COMMENT '下一步动作时间',
  `lost_reason` VARCHAR(500) NULL DEFAULT NULL COMMENT '流失原因（终态 LOST 时填）',
  `last_stage_changed_at` DATETIME NULL DEFAULT NULL COMMENT '最近一次阶段变更时间',
  `create_by` BIGINT NULL DEFAULT NULL,
  `update_by` BIGINT NULL DEFAULT NULL,
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_opp_customer` (`customer_id`) USING BTREE,
  INDEX `idx_opp_project` (`project_id`) USING BTREE,
  INDEX `idx_opp_stage` (`stage_id`) USING BTREE,
  INDEX `idx_opp_owner` (`owner_id`) USING BTREE,
  CONSTRAINT `fk_opp_stage` FOREIGN KEY (`stage_id`) REFERENCES `realty_pipeline_stage` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售商机' ROW_FORMAT = DYNAMIC;

-- 3. 商机阶段流转日志（审计 + 可视化时间线）
CREATE TABLE IF NOT EXISTS `realty_opportunity_stage_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `opportunity_id` BIGINT NOT NULL COMMENT '商机ID',
  `from_stage_id` BIGINT NULL DEFAULT NULL COMMENT '原阶段ID（创建时为空）',
  `to_stage_id` BIGINT NOT NULL COMMENT '新阶段ID',
  `operator_id` BIGINT NULL DEFAULT NULL COMMENT '操作人',
  `remark` VARCHAR(500) NULL DEFAULT NULL,
  `change_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_osl_opportunity` (`opportunity_id`) USING BTREE,
  INDEX `idx_osl_change_time` (`change_time`) USING BTREE,
  CONSTRAINT `fk_osl_opp` FOREIGN KEY (`opportunity_id`) REFERENCES `realty_opportunity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商机阶段流转日志' ROW_FORMAT = DYNAMIC;
