-- ========================================================
-- V43__tenant_invite.sql
-- V4.0 MVP 闭环 ① 账户与组织：
--   1) 租户/企业表 tenant
--   2) 租户成员邀请表 tenant_member_invite（多渠道：短信/链接/二维码/Excel）
--   3) 行业角色 seed（老板/主管/销售/HR/财务/管理员）
-- 备注：
--   * V10/V11 已被早期 chat_enhance / smart_table 占用，V41 已建 sys_org_invite；
--     本次 D1 任务使用 V43 起步以保持 Flyway 单调递增（详见交付文档第 5 项）。
--   * 与 V41 sys_org_invite 互不冲突——本表 tenant_member_invite 多了 tenant_id 与
--     channel 字段，定位"企业一分钟开通后多渠道成员邀请"，保留 V41 简化邀请。
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1) tenant 企业/租户主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tenant` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`           VARCHAR(128) NOT NULL COMMENT '企业名称',
    `code`           VARCHAR(64)  NOT NULL COMMENT '企业编码（短链/二维码用）',
    `industry`       VARCHAR(64)  NULL DEFAULT NULL COMMENT '行业',
    `scale`          VARCHAR(32)  NULL DEFAULT NULL COMMENT '规模：1-50 / 51-200 / 201-1000 / 1000+',
    `admin_user_id`  BIGINT       NOT NULL COMMENT '管理员用户ID',
    `default_dept_id` BIGINT      NULL DEFAULT NULL COMMENT '默认部门ID',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-停用 1-启用',
    `expire_at`      DATETIME     NULL DEFAULT NULL COMMENT '试用/到期时间',
    `remark`         VARCHAR(255) NULL DEFAULT NULL COMMENT '备注',
    `create_by`      BIGINT       NULL DEFAULT NULL COMMENT '创建人',
    `update_by`      BIGINT       NULL DEFAULT NULL COMMENT '更新人',
    `create_time`    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_tenant_code` (`code`) USING BTREE,
    INDEX `idx_tenant_admin_user_id` (`admin_user_id`) USING BTREE,
    INDEX `idx_tenant_status` (`status`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '租户/企业表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 2) tenant_member_invite 多渠道成员邀请
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tenant_member_invite` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`        BIGINT       NOT NULL COMMENT '租户ID',
    `invite_code`      VARCHAR(32)  NOT NULL COMMENT '邀请码',
    `channel`          VARCHAR(16)  NOT NULL DEFAULT 'LINK' COMMENT '邀请渠道: SMS/LINK/QRCODE/EXCEL',
    `phone`            VARCHAR(32)  NULL DEFAULT NULL COMMENT '被邀请手机号（SMS/EXCEL 用）',
    `email`            VARCHAR(128) NULL DEFAULT NULL COMMENT '被邀请邮箱',
    `dept_id`          BIGINT       NULL DEFAULT NULL COMMENT '默认部门',
    `role_ids`         VARCHAR(255) NULL DEFAULT NULL COMMENT '角色ID列表，逗号分隔',
    `inviter_id`       BIGINT       NOT NULL COMMENT '邀请人ID',
    `expires_at`       DATETIME     NOT NULL COMMENT '过期时间',
    `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待接受 1-已接受 2-已撤销 3-已过期',
    `accepted_user_id` BIGINT       NULL DEFAULT NULL COMMENT '接受人用户ID',
    `accepted_at`      DATETIME     NULL DEFAULT NULL COMMENT '接受时间',
    `fail_reason`      VARCHAR(255) NULL DEFAULT NULL COMMENT '失败原因（Excel 行回写）',
    `create_by`        BIGINT       NULL DEFAULT NULL COMMENT '创建人',
    `update_by`        BIGINT       NULL DEFAULT NULL COMMENT '更新人',
    `create_time`      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_tenant_member_invite_code` (`invite_code`) USING BTREE,
    INDEX `idx_tenant_member_invite_tenant` (`tenant_id`, `status`) USING BTREE,
    INDEX `idx_tenant_member_invite_phone` (`phone`) USING BTREE,
    INDEX `idx_tenant_member_invite_expires_at` (`expires_at`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '租户成员邀请表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 3) 预置 6 个行业角色（老板/主管/销售/HR/财务/管理员）
-- ----------------------------
-- 仅在角色不存在时插入，使用 INSERT IGNORE + UNIQUE-by-code（status+deleted 索引）
INSERT INTO `sys_role` (`name`, `code`, `sort`, `status`, `data_scope`, `remark`, `deleted`)
SELECT * FROM (
    SELECT '老板'    AS name, 'tenant_boss'    AS code, 1 AS sort, 1 AS status, 1 AS data_scope, 'V4.0 预置：可见全部数据' AS remark, 0 AS deleted UNION ALL
    SELECT '主管',         'tenant_manager',     2, 1, 4, 'V4.0 预置：本部门及下级',                         0 UNION ALL
    SELECT '销售',         'tenant_sales',       3, 1, 5, 'V4.0 预置：仅本人',                                 0 UNION ALL
    SELECT 'HR',           'tenant_hr',          4, 1, 1, 'V4.0 预置：HR 可见全部',                            0 UNION ALL
    SELECT '财务',         'tenant_finance',     5, 1, 1, 'V4.0 预置：财务可见全部',                          0 UNION ALL
    SELECT '管理员',       'tenant_admin',       6, 1, 1, 'V4.0 预置：管理员可见全部',                        0
) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` r WHERE r.`code` = t.code
);

SET FOREIGN_KEY_CHECKS = 1;
