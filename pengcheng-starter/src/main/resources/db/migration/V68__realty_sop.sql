-- V68: 带看 SOP — 带看确认书 + 佣金三方单（电子签）
-- Phase 5 K3 任务

-- 带看 SOP 实例（每次客户带看一次的记录）
CREATE TABLE realty_visit_sop (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id   BIGINT NOT NULL,
    project_id    BIGINT NOT NULL,
    salesperson_id BIGINT NOT NULL COMMENT '陪同销售',
    alliance_id   BIGINT COMMENT '渠道（如有）',
    visit_time    DATETIME NOT NULL,
    visit_unit_id BIGINT COMMENT '主推房源',
    duration_min  INT COMMENT '带看时长（分钟）',
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING_CONFIRM'
                  COMMENT 'PENDING_CONFIRM/CONFIRMED/EXPIRED/CANCELLED',
    confirm_doc_url VARCHAR(512) COMMENT '带看确认书 PDF（生成后写入）',
    confirm_sign_id VARCHAR(128) COMMENT 'e签宝签署流 ID',
    confirmed_at  DATETIME,
    expires_at    DATETIME COMMENT '确认书有效期（默认 14 天）',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_customer (customer_id, visit_time),
    KEY idx_alliance (alliance_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='带看 SOP 实例';

-- 佣金三方单（开发商/渠道商/客户）—— 客户成交后发起
CREATE TABLE realty_commission_tripartite (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    deal_id         BIGINT NOT NULL UNIQUE COMMENT '成交单 ID（唯一）',
    visit_sop_id    BIGINT COMMENT '关联的带看 SOP（用于风控核对）',
    customer_id     BIGINT NOT NULL,
    project_id      BIGINT NOT NULL,
    alliance_id     BIGINT NOT NULL,
    deal_amount     DECIMAL(15,2) NOT NULL,
    commission_rate DECIMAL(5,4) NOT NULL,
    commission_amount DECIMAL(15,2) NOT NULL,
    party_a_name    VARCHAR(128) NOT NULL COMMENT '甲方（开发商）',
    party_b_name    VARCHAR(128) NOT NULL COMMENT '乙方（渠道）',
    party_c_name    VARCHAR(128) NOT NULL COMMENT '丙方（客户）',
    doc_url         VARCHAR(512),
    sign_flow_id    VARCHAR(128) COMMENT 'e签宝签署流 ID',
    sign_status     VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
                    COMMENT 'DRAFT/SIGNING/SIGNED/REJECTED/EXPIRED',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_alliance_status (alliance_id, sign_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金三方协议';

-- 文档模板（PDF 生成模板，存 HTML/Markdown 占位符模板）
CREATE TABLE realty_sop_template (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(32) NOT NULL UNIQUE COMMENT 'visit_confirm/commission_tripartite',
    name         VARCHAR(128) NOT NULL,
    content_html LONGTEXT NOT NULL COMMENT 'HTML 含 {{var}} 占位符',
    variables    TEXT COMMENT 'JSON 数组：[{key,label,sample}]',
    enabled      TINYINT NOT NULL DEFAULT 1,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='带看 SOP 文档模板';

-- 预置 2 个模板
INSERT INTO realty_sop_template (code, name, content_html, variables) VALUES
('visit_confirm', '带看确认书',
  '<h2>带看确认书</h2><p>客户：{{customer_name}}（手机：{{customer_phone}}）</p><p>楼盘：{{project_name}}</p><p>带看时间：{{visit_time}}</p><p>陪同销售：{{salesperson_name}}</p><p>渠道：{{alliance_name}}</p><p>本人确认上述带看属实，渠道为 {{alliance_name}}。</p><p>有效期：{{expires_at}}</p>',
  '[{"key":"customer_name","label":"客户姓名","sample":"张三"},{"key":"customer_phone","label":"客户手机","sample":"138****1234"},{"key":"project_name","label":"楼盘","sample":"江湾华庭"},{"key":"visit_time","label":"带看时间","sample":"2026-04-27 10:00"},{"key":"salesperson_name","label":"陪同销售","sample":"李四"},{"key":"alliance_name","label":"渠道","sample":"链家地产"},{"key":"expires_at","label":"有效期","sample":"2026-05-11"}]'),
('commission_tripartite', '佣金三方协议',
  '<h2>佣金三方协议</h2><p>甲方（开发商）：{{party_a}}<br>乙方（渠道商）：{{party_b}}<br>丙方（客户）：{{party_c}}</p><p>三方就 {{customer_name}} 购买 {{project_name}} {{full_no}} 房源，成交价 {{deal_amount}} 元达成佣金分配协议：</p><p>佣金费率：{{commission_rate}}%<br>佣金总额：{{commission_amount}} 元</p><p>本协议自三方签署后生效。</p>',
  '[{"key":"party_a","label":"甲方","sample":"XX 房地产"},{"key":"party_b","label":"乙方","sample":"YY 中介"},{"key":"party_c","label":"丙方","sample":"张三"},{"key":"customer_name","label":"客户","sample":"张三"},{"key":"project_name","label":"楼盘","sample":"江湾华庭"},{"key":"full_no","label":"房源","sample":"1-3-0501"},{"key":"deal_amount","label":"成交价","sample":"3500000"},{"key":"commission_rate","label":"费率","sample":"1.5"},{"key":"commission_amount","label":"佣金","sample":"52500"}]');
