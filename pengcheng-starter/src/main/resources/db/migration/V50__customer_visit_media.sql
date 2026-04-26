-- =====================================================
-- V45__customer_visit_media.sql
-- V4.0 闭环③ - customer_visit 扩展多媒体跟进字段
-- 红线：通过加列方式扩展，不重建/不破坏 V1__realty_init.sql 创建的表
-- =====================================================

SET NAMES utf8mb4;

-- 加列：媒体类型/媒体地址列表/语音时长
-- 注意：MySQL 不支持 IF NOT EXISTS 加列，故使用条件存储过程兜底，使脚本可重入。
DROP PROCEDURE IF EXISTS `pc_alter_customer_visit_media`;
DELIMITER $$
CREATE PROCEDURE `pc_alter_customer_visit_media`()
BEGIN
    IF NOT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_visit' AND COLUMN_NAME = 'media_type') THEN
        ALTER TABLE `customer_visit`
            ADD COLUMN `media_type` VARCHAR(20) NULL DEFAULT NULL COMMENT '媒体类型：text/image/audio/video/mixed' AFTER `remark`;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_visit' AND COLUMN_NAME = 'media_urls') THEN
        ALTER TABLE `customer_visit`
            ADD COLUMN `media_urls` JSON NULL COMMENT '媒体 URL 列表（MinIO Key 数组）' AFTER `media_type`;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_visit' AND COLUMN_NAME = 'voice_duration') THEN
        ALTER TABLE `customer_visit`
            ADD COLUMN `voice_duration` INT NULL DEFAULT NULL COMMENT '语音时长（秒）' AFTER `media_urls`;
    END IF;
END $$
DELIMITER ;

CALL `pc_alter_customer_visit_media`();
DROP PROCEDURE IF EXISTS `pc_alter_customer_visit_media`;

-- =====================================================
-- 回滚：
--   ALTER TABLE `customer_visit` DROP COLUMN `voice_duration`;
--   ALTER TABLE `customer_visit` DROP COLUMN `media_urls`;
--   ALTER TABLE `customer_visit` DROP COLUMN `media_type`;
-- =====================================================
