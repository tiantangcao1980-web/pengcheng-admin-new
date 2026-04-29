-- ----------------------------
-- V15: 考勤补强 — GPS / 拍照 / 多围栏
-- 任务来源: V1.0 Sprint C 考勤补强（4 人日）
-- 仅 pengcheng-hr 模块；表 attendance_record 加 GPS/照片/外勤标记字段；新建 attendance_geo_fence 多围栏表
-- ----------------------------

-- ========== 1. attendance_record 增量字段 ==========
ALTER TABLE `attendance_record`
  ADD COLUMN `clock_in_lng`         DECIMAL(10,7) NULL COMMENT '上班打卡经度' AFTER `clock_in_location`,
  ADD COLUMN `clock_in_lat`         DECIMAL(10,7) NULL COMMENT '上班打卡纬度' AFTER `clock_in_lng`,
  ADD COLUMN `clock_in_photo_url`   VARCHAR(500)  NULL COMMENT '上班打卡照片URL' AFTER `clock_in_lat`,
  ADD COLUMN `clock_in_field_work`  TINYINT       NOT NULL DEFAULT 0 COMMENT '上班打卡是否外勤(0=内勤,1=外勤)' AFTER `clock_in_photo_url`,
  ADD COLUMN `clock_out_lng`        DECIMAL(10,7) NULL COMMENT '下班打卡经度' AFTER `clock_out_location`,
  ADD COLUMN `clock_out_lat`        DECIMAL(10,7) NULL COMMENT '下班打卡纬度' AFTER `clock_out_lng`,
  ADD COLUMN `clock_out_photo_url`  VARCHAR(500)  NULL COMMENT '下班打卡照片URL' AFTER `clock_out_lat`,
  ADD COLUMN `clock_out_field_work` TINYINT       NOT NULL DEFAULT 0 COMMENT '下班打卡是否外勤(0=内勤,1=外勤)' AFTER `clock_out_photo_url`;

-- ========== 2. 多围栏表 attendance_geo_fence ==========
CREATE TABLE IF NOT EXISTS `attendance_geo_fence` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`           VARCHAR(100)  NOT NULL COMMENT '围栏名称',
  `center_lng`     DECIMAL(10,7) NOT NULL COMMENT '中心点经度',
  `center_lat`     DECIMAL(10,7) NOT NULL COMMENT '中心点纬度',
  `radius_meters`  INT           NOT NULL COMMENT '半径(米)',
  `active`         TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用,1=启用)',
  `extra`          JSON          NULL COMMENT '扩展字段(JSON)',
  `create_time`    DATETIME      NULL COMMENT '创建时间',
  `update_time`    DATETIME      NULL COMMENT '更新时间',
  `create_by`      BIGINT        NULL COMMENT '创建人',
  `update_by`      BIGINT        NULL COMMENT '更新人',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤地理围栏';

-- ========== 3. 样例围栏（公司总部，半径 200 米） ==========
INSERT INTO `attendance_geo_fence` (name, center_lng, center_lat, radius_meters, active, create_time, update_time)
VALUES ('公司总部', 116.397428, 39.90923, 200, 1, NOW(), NOW());
