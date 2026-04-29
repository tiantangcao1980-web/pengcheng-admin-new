-- ----------------------------
-- V12: 轻量工单系统（IT/HR 报修等内部流转，不引 Flowable）
-- 关联 V1.0 Sprint B 第 5 任务：轻量工单
-- ----------------------------

-- 1. 工单主表
CREATE TABLE IF NOT EXISTS `sys_ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `ticket_no` VARCHAR(32) NOT NULL COMMENT '工单编号 TKT-yyyymmdd-xxxx',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NULL COMMENT '问题描述',
  `category` VARCHAR(32) NOT NULL COMMENT '类型：IT/HR/FINANCE/OTHER',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1低 2中 3高 4紧急',
  `status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED',
  `submitter_id` BIGINT NOT NULL COMMENT '提单人',
  `assignee_id` BIGINT NULL DEFAULT NULL COMMENT '当前处理人',
  `resolved_at` DATETIME NULL DEFAULT NULL COMMENT '解决时间',
  `closed_at` DATETIME NULL DEFAULT NULL COMMENT '关闭时间',
  `extra` JSON NULL DEFAULT NULL COMMENT '扩展字段（附件URLs/关联客户ID等）',
  `create_by` BIGINT NULL DEFAULT NULL,
  `update_by` BIGINT NULL DEFAULT NULL,
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_no` (`ticket_no`) USING BTREE,
  INDEX `idx_ticket_submitter` (`submitter_id`) USING BTREE,
  INDEX `idx_ticket_assignee` (`assignee_id`) USING BTREE,
  INDEX `idx_ticket_status` (`status`) USING BTREE,
  INDEX `idx_ticket_category` (`category`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '轻量工单主表' ROW_FORMAT = DYNAMIC;

-- 2. 工单操作日志（提单/分配/转交/回复/解决/关闭/取消）
CREATE TABLE IF NOT EXISTS `sys_ticket_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `ticket_id` BIGINT NOT NULL COMMENT '工单ID',
  `action` VARCHAR(32) NOT NULL COMMENT '动作：CREATE/ASSIGN/REPLY/RESOLVE/CLOSE/CANCEL/REOPEN',
  `from_status` VARCHAR(32) NULL DEFAULT NULL COMMENT '原状态',
  `to_status` VARCHAR(32) NULL DEFAULT NULL COMMENT '新状态',
  `operator_id` BIGINT NOT NULL COMMENT '操作人',
  `content` VARCHAR(1000) NULL DEFAULT NULL COMMENT '动作内容/回复正文',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tl_ticket` (`ticket_id`) USING BTREE,
  INDEX `idx_tl_action` (`action`) USING BTREE,
  CONSTRAINT `fk_tl_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `sys_ticket` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '轻量工单操作日志' ROW_FORMAT = DYNAMIC;
