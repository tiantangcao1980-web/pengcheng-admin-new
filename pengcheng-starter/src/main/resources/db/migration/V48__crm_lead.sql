-- =====================================================
-- V43__crm_lead.sql
-- V4.0 闭环③ - 线索（Lead）独立主表 + 公开采集表单
-- 任务红线：原任务文档要求 V15-V19，但仓库 V15-V42 已被占用
--          故改用 V43-V47（详见 docs/V4-MVP-D3-DELIVERY.md "红线模糊"）
-- =====================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 线索主表 crm_lead
-- ----------------------------
CREATE TABLE IF NOT EXISTS `crm_lead` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '线索ID',
    `lead_no`           VARCHAR(64)  NOT NULL COMMENT '线索编号（业务可读）',
    `name`              VARCHAR(100) NOT NULL COMMENT '线索名称（联系人姓名）',
    `phone`             VARCHAR(255) NULL DEFAULT NULL COMMENT '联系方式（建议加密存储）',
    `phone_masked`      VARCHAR(20)  NULL DEFAULT NULL COMMENT '脱敏手机号',
    `email`             VARCHAR(120) NULL DEFAULT NULL COMMENT '邮箱',
    `wechat`            VARCHAR(64)  NULL DEFAULT NULL COMMENT '微信号',
    `company`           VARCHAR(200) NULL DEFAULT NULL COMMENT '公司名',
    `source`            VARCHAR(40)  NULL DEFAULT NULL COMMENT '线索来源（form/qrcode/import/manual/api）',
    `source_detail`     VARCHAR(255) NULL DEFAULT NULL COMMENT '来源明细（活动/页面/UTM）',
    `intention_level`   TINYINT      NOT NULL DEFAULT 2 COMMENT '意向等级：1-高 2-中 3-低',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-待分配 2-已分配 3-跟进中 4-已转客户 5-已废弃',
    `owner_id`          BIGINT       NULL DEFAULT NULL COMMENT '当前负责人（sys_user.id）',
    `dept_id`           BIGINT       NULL DEFAULT NULL COMMENT '当前部门',
    `assign_time`       DATETIME     NULL DEFAULT NULL COMMENT '最近分配时间',
    `last_follow_time`  DATETIME     NULL DEFAULT NULL COMMENT '最近跟进时间',
    `convert_time`      DATETIME     NULL DEFAULT NULL COMMENT '转客户时间',
    `customer_id`       BIGINT       NULL DEFAULT NULL COMMENT '转化后的客户ID',
    `remark`            VARCHAR(1000) NULL DEFAULT NULL COMMENT '备注',
    `tenant_id`         BIGINT       NULL DEFAULT NULL COMMENT '租户ID（预留）',
    `create_by`         BIGINT       NULL DEFAULT NULL,
    `update_by`         BIGINT       NULL DEFAULT NULL,
    `create_time`       DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_crm_lead_no` (`lead_no`) USING BTREE,
    KEY `idx_crm_lead_owner` (`owner_id`) USING BTREE,
    KEY `idx_crm_lead_status` (`status`) USING BTREE,
    KEY `idx_crm_lead_phone` (`phone`(32)) USING BTREE,
    KEY `idx_crm_lead_source` (`source`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM 线索主表';

-- ----------------------------
-- 2. 线索分配/流转日志 crm_lead_assignment
-- ----------------------------
CREATE TABLE IF NOT EXISTS `crm_lead_assignment` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `lead_id`         BIGINT      NOT NULL,
    `from_user_id`    BIGINT      NULL DEFAULT NULL COMMENT '原负责人',
    `to_user_id`      BIGINT      NOT NULL COMMENT '新负责人',
    `assigned_by`     BIGINT      NULL DEFAULT NULL COMMENT '操作人（管理员/规则引擎）',
    `rule_type`       VARCHAR(20) NULL DEFAULT 'manual' COMMENT '分配方式：manual/round_robin/load_balance/rule',
    `note`            VARCHAR(255) NULL DEFAULT NULL,
    `create_time`     DATETIME    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_crm_lead_assign_lead` (`lead_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM 线索分配/流转记录';

-- ----------------------------
-- 3. 公开采集表单（简化版表单构建器） crm_lead_form
-- ----------------------------
CREATE TABLE IF NOT EXISTS `crm_lead_form` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `form_code`       VARCHAR(64)  NOT NULL COMMENT '公开 URL slug，唯一',
    `title`           VARCHAR(120) NOT NULL,
    `description`     VARCHAR(500) NULL DEFAULT NULL,
    `schema_json`     JSON         NOT NULL COMMENT '字段定义 JSON：[{key,label,type,required,options,...}]',
    `default_owner_id` BIGINT      NULL DEFAULT NULL COMMENT '默认归属人',
    `default_source`  VARCHAR(40)  NULL DEFAULT 'form',
    `qrcode_url`      VARCHAR(500) NULL DEFAULT NULL COMMENT '二维码图片地址（按需生成/缓存）',
    `enabled`         TINYINT      NOT NULL DEFAULT 1,
    `submit_count`    INT          NOT NULL DEFAULT 0,
    `tenant_id`       BIGINT       NULL DEFAULT NULL,
    `create_by`       BIGINT       NULL DEFAULT NULL,
    `update_by`       BIGINT       NULL DEFAULT NULL,
    `create_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_crm_lead_form_code` (`form_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM 公开线索采集表单';

-- =====================================================
-- 回滚脚本（请勿在生产环境直接执行）：
--   DROP TABLE IF EXISTS `crm_lead_form`;
--   DROP TABLE IF EXISTS `crm_lead_assignment`;
--   DROP TABLE IF EXISTS `crm_lead`;
-- =====================================================
