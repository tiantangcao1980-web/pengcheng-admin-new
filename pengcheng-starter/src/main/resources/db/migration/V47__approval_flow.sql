-- V45: 审批模板 + 流程定义 + 节点 + 实例 + 记录（V4.0 闭环②，D2 任务）

-- ===== 1. 审批模板 =====
CREATE TABLE IF NOT EXISTS approval_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL COMMENT '业务编码（唯一）',
    name VARCHAR(128) NOT NULL COMMENT '模板名称',
    form_schema TEXT NULL COMMENT '表单 schema (JSON)',
    default_flow_def_id BIGINT NULL COMMENT '默认流程定义 ID',
    category TINYINT NOT NULL COMMENT '分类 1=假勤 2=出差 3=费用 4=通用',
    enabled TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (code),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批模板';

-- 内置 5 类模板（请假/外出/加班/报销/通用）
INSERT INTO approval_template (code, name, category, enabled, remark, create_time, update_time)
SELECT 'leave', '请假申请', 1, 1, '请假（事假/病假/年假等）', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM approval_template WHERE code = 'leave');

INSERT INTO approval_template (code, name, category, enabled, remark, create_time, update_time)
SELECT 'outing', '外出申请', 2, 1, '外出报备', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM approval_template WHERE code = 'outing');

INSERT INTO approval_template (code, name, category, enabled, remark, create_time, update_time)
SELECT 'overtime', '加班申请', 1, 1, '加班调休', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM approval_template WHERE code = 'overtime');

INSERT INTO approval_template (code, name, category, enabled, remark, create_time, update_time)
SELECT 'reimburse', '报销申请', 3, 1, '通用费用报销', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM approval_template WHERE code = 'reimburse');

INSERT INTO approval_template (code, name, category, enabled, remark, create_time, update_time)
SELECT 'general', '通用审批', 4, 1, '通用流程模板，用于未定义业务', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM approval_template WHERE code = 'general');

INSERT INTO approval_template (code, name, category, enabled, remark, create_time, update_time)
SELECT 'correction', '补卡申请', 1, 1, 'D2 闭环②补卡单', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM approval_template WHERE code = 'correction');

-- ===== 2. 流程定义 =====
CREATE TABLE IF NOT EXISTS approval_flow_def (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型（与 approval_template.code 对齐）',
    name VARCHAR(128) NOT NULL COMMENT '流程名称',
    enabled TINYINT NOT NULL DEFAULT 1,
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认流程',
    remark VARCHAR(255) NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_biz_type (biz_type),
    KEY idx_default (biz_type, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程定义';

-- ===== 3. 流程节点 =====
CREATE TABLE IF NOT EXISTS approval_flow_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_def_id BIGINT NOT NULL COMMENT '流程定义 ID',
    node_order INT NOT NULL COMMENT '节点顺序，从 1 开始',
    node_name VARCHAR(64) NOT NULL,
    node_type TINYINT NOT NULL DEFAULT 1 COMMENT '1=指定用户 2=部门主管 3=角色',
    approver_ids VARCHAR(500) NULL COMMENT '审批人 ID 列表（逗号分隔）',
    role_key VARCHAR(64) NULL,
    timeout_hours INT NULL COMMENT '超时小时数（0/null 不超时）',
    timeout_action TINYINT NULL COMMENT '超时策略 1=自动通过 2=自动驳回 3=跳过',
    allow_add_sign TINYINT NULL DEFAULT 0,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_flow_def_order (flow_def_id, node_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程节点';

-- ===== 4. 流程实例 =====
CREATE TABLE IF NOT EXISTS approval_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_def_id BIGINT NOT NULL,
    biz_type VARCHAR(64) NOT NULL,
    biz_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    current_node_order INT NULL,
    current_node_id BIGINT NULL,
    state TINYINT NOT NULL DEFAULT 1 COMMENT '1=运行 2=通过 3=驳回 4=取消',
    summary VARCHAR(500) NULL,
    current_node_deadline DATETIME NULL,
    end_time DATETIME NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_state (state),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_applicant (applicant_id),
    KEY idx_deadline (current_node_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程实例';

-- ===== 5. 单步审批记录 =====
CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL,
    node_order INT NOT NULL,
    approver_id BIGINT NULL,
    result TINYINT NOT NULL COMMENT '1=通过 2=驳回 3=超时通过 4=超时驳回 5=超时跳过',
    remark VARCHAR(500) NULL,
    action_time DATETIME NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_instance (instance_id),
    KEY idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批单步记录';

-- ===== 6. 回填 default_flow_def_id 的反向关联，需在 def 创建后由业务侧补 =====
-- 此处不预置流程节点，留给管理员通过 Web 端可视化配置页面创建
