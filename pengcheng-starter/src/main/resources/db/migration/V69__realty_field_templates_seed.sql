-- V69: 房产行业预置字段模板 seed
-- 复用 D3 的 custom_field_def 表，插入房产行业扩展字段
-- 注意：字段列以实体实际列名为准（label / sort_order）

-- 行业字段模板：房产 lead 扩展
INSERT INTO custom_field_def (entity_type, field_key, label, field_type, options_json, required, sort_order, enabled, description)
VALUES
('lead', 'realty_intent_area',    '意向面积', 'number', NULL, 0, 100, 1, '房产线索：意向购房面积 m²'),
('lead', 'realty_budget',         '预算',     'number', NULL, 0, 101, 1, '房产线索：购房预算（万元）'),
('lead', 'realty_preferred_floor','楼层偏好', 'select', '[{"value":"low","label":"低层(1-5)"},{"value":"mid","label":"中层(6-15)"},{"value":"high","label":"高层(16+)"}]', 0, 102, 1, '房产线索：楼层偏好'),
('lead', 'realty_intent_project', '意向楼盘', 'multi_select', NULL, 0, 103, 1, '房产线索：候选楼盘列表（联动 project 表）'),
('lead', 'realty_purpose',        '购房用途', 'select', '[{"value":"self","label":"自住"},{"value":"invest","label":"投资"},{"value":"upgrade","label":"改善"}]', 0, 104, 1, '房产线索：购房用途'),

-- 客户扩展
('customer', 'realty_intent_area',       '意向面积', 'number', NULL, 0, 100, 1, NULL),
('customer', 'realty_budget',            '预算',     'number', NULL, 0, 101, 1, NULL),
('customer', 'realty_first_visit_date',  '首次到访', 'date',   NULL, 0, 102, 1, NULL),
('customer', 'realty_decision_role',     '决策角色', 'select', '[{"value":"buyer","label":"购房人"},{"value":"family","label":"家属"},{"value":"investor","label":"投资人"}]', 0, 103, 1, NULL),
('customer', 'realty_pay_method',        '付款方式', 'select', '[{"value":"cash","label":"全款"},{"value":"loan","label":"按揭"},{"value":"combo","label":"组合"}]', 0, 104, 1, NULL),

-- 成交扩展
('opportunity', 'realty_unit_id',         '签约房源', 'number', NULL, 0, 100, 1, '关联 realty_unit.id'),
('opportunity', 'realty_house_type_code', '户型',     'text',   NULL, 0, 101, 1, NULL),
('opportunity', 'realty_pay_progress',    '付款进度', 'select', '[{"value":"deposit","label":"定金"},{"value":"down","label":"首付"},{"value":"loan_approved","label":"贷款批准"},{"value":"full","label":"全款到账"}]', 0, 102, 1, NULL);

-- 模板分组（用于一键导入插件时整批启用/禁用）
CREATE TABLE custom_field_template_group (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    plugin_code   VARCHAR(64)  NOT NULL             COMMENT '行业插件编码，如 realty',
    entity_type   VARCHAR(32)  NOT NULL             COMMENT '实体类型：lead / customer / opportunity',
    template_name VARCHAR(128) NOT NULL             COMMENT '模板名称',
    field_keys    TEXT         NOT NULL             COMMENT '逗号分隔的 field_key 列表',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_plugin (plugin_code, entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业字段模板分组';

INSERT INTO custom_field_template_group (plugin_code, entity_type, template_name, field_keys) VALUES
('realty', 'lead',        '房产线索扩展', 'realty_intent_area,realty_budget,realty_preferred_floor,realty_intent_project,realty_purpose'),
('realty', 'customer',    '房产客户扩展', 'realty_intent_area,realty_budget,realty_first_visit_date,realty_decision_role,realty_pay_method'),
('realty', 'opportunity', '房产成交扩展', 'realty_unit_id,realty_house_type_code,realty_pay_progress');
