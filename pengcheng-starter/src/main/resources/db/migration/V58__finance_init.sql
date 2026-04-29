-- ============================================================
-- V58: 财务闭环骨架初始化
--   合同管理：contract / contract_template / contract_version / contract_sign_record
--   发票管理：invoice / invoice_item / invoice_delivery
--   通用提成：commission_rule / commission_record
--
-- 说明：
--   1. 表前缀统一用 contract_ / invoice_ / commission_，
--      与 realty 模块的 project_commission_rule（房产专属）区分。
--   2. customer_deal.contract_no 字段原样保留，不受此迁移影响。
--   3. 外键引用 customer（客户主表）和 customer_deal（成交主表）逻辑约束，
--      不建硬外键，保持灵活性。
-- ============================================================

-- ============================================================
-- 1. 合同模板（contract_template）
--    提供合同起草所用的文本模板与变量声明。
-- ============================================================
CREATE TABLE IF NOT EXISTS contract_template
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           VARCHAR(128)    NOT NULL COMMENT '模板名称',
    biz_type       VARCHAR(64)     NOT NULL COMMENT '业务类型（如 realty/crm/general）',
    content        LONGTEXT        NOT NULL COMMENT '模板正文（支持 {{变量}} 占位符）',
    variables_json JSON                     COMMENT '变量定义 JSON 数组，示例：[{"key":"partyA","label":"甲方名称"}]',
    version        INT             NOT NULL DEFAULT 1 COMMENT '模板版本号',
    active         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用 0=停用',
    create_by      BIGINT UNSIGNED          COMMENT '创建人 user_id',
    update_by      BIGINT UNSIGNED          COMMENT '更新人 user_id',
    create_time    DATETIME                 COMMENT '创建时间',
    update_time    DATETIME                 COMMENT '更新时间',
    deleted        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    PRIMARY KEY (id),
    KEY idx_biz_type (biz_type),
    KEY idx_active (active)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '合同模板';

-- ============================================================
-- 2. 合同主表（contract）
--    一份合同对应一个客户 / 一笔成交，全生命周期管理。
-- ============================================================
CREATE TABLE IF NOT EXISTS contract
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    contract_no      VARCHAR(64)     NOT NULL COMMENT '合同编号（系统生成，全局唯一）',
    title            VARCHAR(256)    NOT NULL COMMENT '合同标题',
    template_id      BIGINT UNSIGNED          COMMENT '来源模板 ID（contract_template.id，允许为空表示自由录入）',
    customer_id      BIGINT UNSIGNED          COMMENT '关联客户 ID（customer.id）',
    deal_id          BIGINT UNSIGNED          COMMENT '关联成交 ID（customer_deal.id）',
    amount           DECIMAL(18, 2)           COMMENT '合同金额',
    status           TINYINT         NOT NULL DEFAULT 1
        COMMENT '合同状态：1=起草 2=审批中 3=审批通过 4=审批拒绝 5=签署中 6=签署完成 7=履约中 8=已归档 9=已作废',
    sign_status      TINYINT         NOT NULL DEFAULT 0
        COMMENT '签署状态：0=未签署 1=部分签署 2=全部签署',
    sign_provider    VARCHAR(32)              COMMENT '电子签服务商：esign（e签宝）/ fadada（法大大）/ offline（线下）',
    external_sign_id VARCHAR(128)             COMMENT '外部签署平台的合同ID（e签宝 flowId 等，Phase 2 后续填充）',
    version          INT             NOT NULL DEFAULT 1 COMMENT '当前版本号',
    remark           VARCHAR(512)             COMMENT '备注',
    create_by        BIGINT UNSIGNED          COMMENT '创建人 user_id',
    update_by        BIGINT UNSIGNED          COMMENT '更新人 user_id',
    create_time      DATETIME                 COMMENT '创建时间',
    update_time      DATETIME                 COMMENT '更新时间',
    deleted          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_no (contract_no),
    KEY idx_customer_id (customer_id),
    KEY idx_deal_id (deal_id),
    KEY idx_status (status),
    KEY idx_sign_status (sign_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '合同主表';

-- ============================================================
-- 3. 合同版本（contract_version）
--    记录合同每次内容变更的快照和差异。
-- ============================================================
CREATE TABLE IF NOT EXISTS contract_version
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    contract_id BIGINT UNSIGNED NOT NULL COMMENT '合同 ID（contract.id）',
    version     INT             NOT NULL COMMENT '版本号',
    content     LONGTEXT        NOT NULL COMMENT '该版本合同全文快照',
    diff        TEXT                     COMMENT '与上一版本的差异描述（可存 unified diff 或摘要）',
    create_by   BIGINT UNSIGNED          COMMENT '创建人 user_id',
    create_time DATETIME                 COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_contract_id (contract_id),
    UNIQUE KEY uk_contract_version (contract_id, version)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '合同版本历史';

-- ============================================================
-- 4. 签署记录（contract_sign_record）
--    记录每位签署方的签署动作（支持多签署人）。
-- ============================================================
CREATE TABLE IF NOT EXISTS contract_sign_record
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    contract_id      BIGINT UNSIGNED NOT NULL COMMENT '合同 ID（contract.id）',
    signer_id        BIGINT UNSIGNED          COMMENT '签署人系统 user_id（内部用户可关联，外部签署人可为 null）',
    signer_name      VARCHAR(64)     NOT NULL COMMENT '签署人姓名',
    signer_role      VARCHAR(32)              COMMENT '签署方角色：partyA=甲方 / partyB=乙方 / witness=见证方',
    sign_time        DATETIME                 COMMENT '签署完成时间',
    sign_provider    VARCHAR(32)              COMMENT '签署服务商：esign / fadada / offline',
    external_sign_id VARCHAR(128)             COMMENT '外部签署平台的签署记录 ID',
    sign_result      TINYINT         NOT NULL DEFAULT 0
        COMMENT '签署结果：0=待签 1=已签 2=已拒签 3=已过期',
    create_time      DATETIME                 COMMENT '记录创建时间',
    PRIMARY KEY (id),
    KEY idx_contract_id (contract_id),
    KEY idx_signer_id (signer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '合同签署记录';

-- ============================================================
-- 5. 发票主表（invoice）
--    申请 → 审批 → 开具的全流程主记录。
-- ============================================================
CREATE TABLE IF NOT EXISTS invoice
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    invoice_no   VARCHAR(64)              COMMENT '发票号码（开具后由税控系统返回填充）',
    contract_id  BIGINT UNSIGNED          COMMENT '关联合同 ID（contract.id，允许为空）',
    customer_id  BIGINT UNSIGNED          COMMENT '购买方客户 ID（customer.id）',
    amount       DECIMAL(18, 2)  NOT NULL COMMENT '开票金额（不含税）',
    tax_rate     DECIMAL(6, 4)   NOT NULL DEFAULT 0.0600 COMMENT '税率（如 0.06 = 6%）',
    tax_amount   DECIMAL(18, 2)           COMMENT '税额（系统计算：amount * tax_rate）',
    total_amount DECIMAL(18, 2)           COMMENT '价税合计',
    invoice_type TINYINT         NOT NULL DEFAULT 1
        COMMENT '发票类型：1=增值税普票 2=增值税专票 3=电子普票 4=电子专票',
    status       TINYINT         NOT NULL DEFAULT 1
        COMMENT '状态：1=申请中 2=审批通过 3=审批拒绝 4=已开具 5=已作废 6=已红冲',
    issue_date   DATE                     COMMENT '开票日期',
    issuer_id    BIGINT UNSIGNED          COMMENT '开票人 user_id',
    remark       VARCHAR(512)             COMMENT '备注',
    create_by    BIGINT UNSIGNED          COMMENT '申请人 user_id',
    update_by    BIGINT UNSIGNED          COMMENT '更新人 user_id',
    create_time  DATETIME                 COMMENT '创建时间',
    update_time  DATETIME                 COMMENT '更新时间',
    deleted      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    PRIMARY KEY (id),
    KEY idx_contract_id (contract_id),
    KEY idx_customer_id (customer_id),
    KEY idx_status (status),
    KEY idx_invoice_no (invoice_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '发票主表';

-- ============================================================
-- 6. 发票明细（invoice_item）
--    发票行项目，对应"货物或服务清单"。
-- ============================================================
CREATE TABLE IF NOT EXISTS invoice_item
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    invoice_id  BIGINT UNSIGNED NOT NULL COMMENT '发票 ID（invoice.id）',
    item_name   VARCHAR(256)    NOT NULL COMMENT '项目名称 / 货物名称',
    spec        VARCHAR(64)              COMMENT '规格型号',
    unit        VARCHAR(16)              COMMENT '计量单位',
    qty         DECIMAL(14, 4)           COMMENT '数量',
    unit_price  DECIMAL(18, 4)           COMMENT '单价（不含税）',
    amount      DECIMAL(18, 2)  NOT NULL COMMENT '金额（不含税）',
    sort        INT             NOT NULL DEFAULT 0 COMMENT '行序号',
    PRIMARY KEY (id),
    KEY idx_invoice_id (invoice_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '发票明细行';

-- ============================================================
-- 7. 发票物流（invoice_delivery）
--    记录发票快递寄送及签收状态。
-- ============================================================
CREATE TABLE IF NOT EXISTS invoice_delivery
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    invoice_id       BIGINT UNSIGNED NOT NULL COMMENT '发票 ID（invoice.id）',
    express_provider VARCHAR(64)              COMMENT '快递公司（如 SF / YTO / ZTO）',
    express_no       VARCHAR(64)              COMMENT '快递单号',
    send_time        DATETIME                 COMMENT '寄出时间',
    sign_status      TINYINT         NOT NULL DEFAULT 0
        COMMENT '签收状态：0=未寄出 1=运输中 2=已签收 3=签收异常',
    sign_time        DATETIME                 COMMENT '客户签收时间',
    receiver_name    VARCHAR(64)              COMMENT '收件人姓名',
    receiver_address VARCHAR(256)             COMMENT '收件地址',
    remark           VARCHAR(256)             COMMENT '备注',
    create_time      DATETIME                 COMMENT '创建时间',
    update_time      DATETIME                 COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_id (invoice_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '发票物流记录';

-- ============================================================
-- 8. 通用提成规则（commission_rule）
--    适用于非房产行业的通用销售提成规则（DSL 表达式驱动）。
--    注意：房产专属规则仍在 realty 模块的 project_commission_rule 表。
-- ============================================================
CREATE TABLE IF NOT EXISTS commission_rule
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_name       VARCHAR(128)    NOT NULL COMMENT '规则名称',
    biz_type        VARCHAR(64)     NOT NULL COMMENT '适用业务类型（如 crm_deal / general）',
    calc_mode       TINYINT         NOT NULL DEFAULT 1
        COMMENT '计算模式：1=固定比例 2=阶梯比例 3=团队分成 4=DSL 表达式',
    rate            DECIMAL(8, 4)            COMMENT '固定比例（calc_mode=1 时有效，如 0.0300=3%）',
    ladder_json     JSON                     COMMENT '阶梯配置 JSON（calc_mode=2）：[{"min":0,"max":100000,"rate":0.03},...]',
    team_split_json JSON                     COMMENT '团队分成配置 JSON（calc_mode=3）：[{"userId":1,"ratio":0.6},...]',
    expression_dsl  TEXT                     COMMENT 'DSL 表达式（calc_mode=4，留给后续规则引擎解析）',
    active          TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用 0=停用',
    remark          VARCHAR(512)             COMMENT '规则说明',
    create_by       BIGINT UNSIGNED          COMMENT '创建人 user_id',
    update_by       BIGINT UNSIGNED          COMMENT '更新人 user_id',
    create_time     DATETIME                 COMMENT '创建时间',
    update_time     DATETIME                 COMMENT '更新时间',
    deleted         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    PRIMARY KEY (id),
    KEY idx_biz_type (biz_type),
    KEY idx_active (active)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '通用销售提成规则（非房产行业；房产专属规则见 project_commission_rule）';

-- ============================================================
-- 9. 通用提成记录（commission_record）
--    每次触发提成计算产生的记录，支持审核流。
-- ============================================================
CREATE TABLE IF NOT EXISTS commission_record
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_id      BIGINT UNSIGNED NOT NULL COMMENT '提成规则 ID（commission_rule.id）',
    sale_user_id BIGINT UNSIGNED NOT NULL COMMENT '销售人员 user_id',
    biz_id       BIGINT UNSIGNED NOT NULL COMMENT '关联业务 ID（如 customer_deal.id / CRM 商机 ID）',
    biz_type     VARCHAR(64)     NOT NULL COMMENT '业务类型（与 rule.biz_type 对应）',
    base_amount  DECIMAL(18, 2)  NOT NULL COMMENT '提成计算基数（通常为成交金额）',
    amount       DECIMAL(18, 2)  NOT NULL COMMENT '应得提成金额',
    audit_status TINYINT         NOT NULL DEFAULT 0
        COMMENT '审核状态：0=待审核 1=审核通过 2=审核拒绝 3=已发放',
    audit_by     BIGINT UNSIGNED          COMMENT '审核人 user_id',
    audit_time   DATETIME                 COMMENT '审核时间',
    audit_remark VARCHAR(256)             COMMENT '审核意见',
    remark       VARCHAR(512)             COMMENT '备注',
    create_by    BIGINT UNSIGNED          COMMENT '创建人 user_id',
    create_time  DATETIME                 COMMENT '创建时间',
    update_time  DATETIME                 COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_rule_id (rule_id),
    KEY idx_sale_user_id (sale_user_id),
    KEY idx_biz_id_type (biz_id, biz_type),
    KEY idx_audit_status (audit_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '通用销售提成记录（非房产行业；房产提成见 commission_record in realty module）';
