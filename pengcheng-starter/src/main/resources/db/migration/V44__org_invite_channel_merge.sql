-- ========================================================
-- V44__org_invite_channel_merge.sql
-- E4：sys_org_invite 扩展多渠道字段，合并 tenant_member_invite 数据后 DROP
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 为 sys_org_invite 增加 4 个新列
ALTER TABLE `sys_org_invite`
    ADD COLUMN `tenant_id`       BIGINT      NULL DEFAULT NULL COMMENT '租户ID'         AFTER `deleted`,
    ADD COLUMN `channel`         VARCHAR(16) NOT NULL DEFAULT 'LINK' COMMENT '邀请渠道：LINK/SMS/QRCODE/EXCEL' AFTER `tenant_id`,
    ADD COLUMN `qrcode_url`      VARCHAR(255) NULL DEFAULT NULL COMMENT '二维码图片URL' AFTER `channel`,
    ADD COLUMN `excel_batch_id`  VARCHAR(40)  NULL DEFAULT NULL COMMENT 'Excel 批次ID（同一批 Excel 导入共享）' AFTER `qrcode_url`;

-- 2. 为新列加索引
ALTER TABLE `sys_org_invite`
    ADD INDEX `idx_sys_org_invite_tenant_id` (`tenant_id`) USING BTREE,
    ADD INDEX `idx_sys_org_invite_channel` (`channel`) USING BTREE;

-- 3. 将 tenant_member_invite 数据迁移到 sys_org_invite
INSERT INTO `sys_org_invite` (
    `invite_code`, `email`, `phone`, `role_ids`, `dept_id`,
    `status`, `expires_at`, `accepted_user_id`, `accepted_at`,
    `create_by`, `update_by`, `create_time`, `update_time`, `deleted`,
    `tenant_id`, `channel`, `qrcode_url`, `excel_batch_id`
)
SELECT
    `invite_code`, `email`, `phone`, `role_ids`, `dept_id`,
    `status`, `expires_at`, `accepted_user_id`, `accepted_at`,
    `create_by`, `update_by`, `create_time`, `update_time`, `deleted`,
    `tenant_id`, `channel`, `qrcode_url`, `excel_batch_id`
FROM `tenant_member_invite`;

-- 4. 删除已完成迁移的临时表
DROP TABLE IF EXISTS `tenant_member_invite`;

SET FOREIGN_KEY_CHECKS = 1;
