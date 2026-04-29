-- ----------------------------
-- V16: 房产销售外勤拜访（V1.0 Sprint C 主线）
-- 与 customer_visit（客户到访案场）解耦：FieldVisit 是销售去客户/楼盘
-- ----------------------------

CREATE TABLE IF NOT EXISTS `attendance_field_visit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '外勤记录ID',
  `user_id` BIGINT NOT NULL COMMENT '业务员ID',
  `customer_id` BIGINT NULL DEFAULT NULL COMMENT '关联客户ID（客户拜访场景）',
  `project_id` BIGINT NULL DEFAULT NULL COMMENT '关联楼盘ID（楼盘踏勘/带看场景）',
  `visit_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1客户拜访 2楼盘踏勘 3带看 4其他',
  `longitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '纬度',
  `address` VARCHAR(200) NULL DEFAULT NULL COMMENT '反向地理编码地址',
  `photo_urls` VARCHAR(1000) NULL DEFAULT NULL COMMENT '拍照 OSS URL 数组（逗号分隔）',
  `purpose` VARCHAR(500) NULL DEFAULT NULL COMMENT '拜访目的（签到时填）',
  `result` VARCHAR(500) NULL DEFAULT NULL COMMENT '拜访结果（签退时补填）',
  `check_in_time` DATETIME NOT NULL COMMENT '签到时间',
  `check_out_time` DATETIME NULL DEFAULT NULL COMMENT '签退时间',
  `duration_minutes` INT NULL DEFAULT NULL COMMENT '停留时长（分钟，签退后计算）',
  `tenant_id` BIGINT NULL DEFAULT NULL COMMENT '租户ID（向前兼容设计纪律）',
  `extra` JSON NULL DEFAULT NULL COMMENT '扩展字段',
  `create_by` BIGINT NULL DEFAULT NULL,
  `update_by` BIGINT NULL DEFAULT NULL,
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_fv_user_time` (`user_id`, `check_in_time` DESC) USING BTREE,
  INDEX `idx_fv_customer` (`customer_id`) USING BTREE,
  INDEX `idx_fv_project` (`project_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售外勤拜访' ROW_FORMAT = DYNAMIC;
