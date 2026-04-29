-- ============================================================
-- V61__bi.sql  BI 自助分析模块（V4.0 Phase 3 I4）
-- ============================================================

-- BI 视图模型（白名单 SQL，限定表 + 允许的维度/指标列，防 SQL 注入）
CREATE TABLE bi_view_model (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    code          VARCHAR(64)  NOT NULL UNIQUE COMMENT '视图编码，如 customer_pool',
    name          VARCHAR(128) NOT NULL               COMMENT '视图名称',
    base_table    VARCHAR(64)  NOT NULL               COMMENT '主表名（白名单）',
    join_clause   VARCHAR(512)                        COMMENT '可选 JOIN 子句（管理员配置）',
    dimensions    TEXT         NOT NULL               COMMENT 'JSON 数组：[{key,label,column,type}]',
    metrics       TEXT         NOT NULL               COMMENT 'JSON 数组：[{key,label,formula(SUM/AVG/COUNT/MAX/MIN/COUNT_DISTINCT),column}]',
    enabled       TINYINT      NOT NULL DEFAULT 1     COMMENT '是否启用',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI 视图模型';

-- 用户保存的查询
CREATE TABLE bi_saved_query (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL               COMMENT '保存人 user_id',
    view_code    VARCHAR(64)  NOT NULL               COMMENT '视图编码',
    name         VARCHAR(128) NOT NULL               COMMENT '查询名称',
    query_json   TEXT         NOT NULL               COMMENT '{dimensions, metrics, filters, sort, limit}',
    create_time  DATETIME              DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI 保存查询';

-- 预置示例视图：客户池分析
INSERT INTO bi_view_model (code, name, base_table, dimensions, metrics, enabled)
VALUES (
    'customer_pool',
    '客户池分析',
    'customer',
    '[{"key":"source","label":"来源","column":"source","type":"string"},{"key":"city","label":"城市","column":"city","type":"string"},{"key":"createMonth","label":"创建月","column":"create_time","type":"date_month"}]',
    '[{"key":"count","label":"客户数","formula":"COUNT","column":"id"},{"key":"avgVisits","label":"平均拜访","formula":"AVG","column":"visit_count"}]',
    1
);
