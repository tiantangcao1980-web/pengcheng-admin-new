-- ----------------------------
-- V10: 佣金多级审批流（业务员 → 主管 → 财务 → 放款）
-- 关联 V1.0 Sprint A 第一任务：佣金审批流闭环
-- 设计原则：保留旧 audit_status 兼容存量数据与旧 API，新增 approval_node 表达多级流转
-- ----------------------------

-- 1. 升级 commission 主表：新增多级审批相关字段
ALTER TABLE `commission`
  ADD COLUMN `approval_node` VARCHAR(32) NULL DEFAULT NULL COMMENT '当前审批节点：DRAFT/SUBMITTED/MANAGER_APPROVED/FINANCE_APPROVED/PAID/REJECTED' AFTER `audit_time`,
  ADD COLUMN `submitted_by` BIGINT NULL DEFAULT NULL COMMENT '提交人ID（业务员）' AFTER `approval_node`,
  ADD COLUMN `submitted_time` DATETIME NULL DEFAULT NULL COMMENT '提交时间' AFTER `submitted_by`,
  ADD COLUMN `paid_by` BIGINT NULL DEFAULT NULL COMMENT '放款操作人ID' AFTER `submitted_time`,
  ADD COLUMN `paid_time` DATETIME NULL DEFAULT NULL COMMENT '放款时间' AFTER `paid_by`,
  ADD INDEX `idx_comm_approval_node` (`approval_node`) USING BTREE;

-- 2. 存量数据迁移：旧 audit_status 映射到新 approval_node（语义保持一致）
--    旧 audit_status=1（待审核）→ 视为已提交（待主管审批）
--    旧 audit_status=2（审核通过）→ 视为已放款（旧流程已结束）
--    旧 audit_status=3（审核驳回）→ 驳回
UPDATE `commission` SET `approval_node` = 'SUBMITTED' WHERE `approval_node` IS NULL AND `audit_status` = 1;
UPDATE `commission` SET `approval_node` = 'PAID'      WHERE `approval_node` IS NULL AND `audit_status` = 2;
UPDATE `commission` SET `approval_node` = 'REJECTED'  WHERE `approval_node` IS NULL AND `audit_status` = 3;

-- 3. 新增 commission_approval 审批节点记录表（参考 payment_approval 设计模式）
CREATE TABLE IF NOT EXISTS `commission_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批记录ID',
  `commission_id` BIGINT NOT NULL COMMENT '佣金ID',
  `node` VARCHAR(32) NOT NULL COMMENT '节点：MANAGER/FINANCE/PAYMENT',
  `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
  `result` TINYINT NOT NULL COMMENT '审批结果：1-通过 2-驳回',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '审批备注（驳回时建议必填）',
  `approval_order` INT NOT NULL COMMENT '审批顺序：1=主管 2=财务 3=放款',
  `approval_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ca_commission_id` (`commission_id`) USING BTREE,
  INDEX `idx_ca_approver_id` (`approver_id`) USING BTREE,
  INDEX `idx_ca_node` (`node`) USING BTREE,
  CONSTRAINT `fk_ca_commission` FOREIGN KEY (`commission_id`) REFERENCES `commission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金审批节点记录表' ROW_FORMAT = DYNAMIC;
