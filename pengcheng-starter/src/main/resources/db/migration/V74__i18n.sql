-- =====================================================================
-- V74: 国际化 i18n（L5 — Phase 6）
-- =====================================================================

-- 用户语言/时区/货币偏好
CREATE TABLE user_locale_preference (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    locale        VARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    timezone      VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    currency      VARCHAR(8)  NOT NULL DEFAULT 'CNY',
    date_format   VARCHAR(32) NOT NULL DEFAULT 'YYYY-MM-DD',
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户区域偏好';

-- 后端动态词条（运行时编辑无需重启）
CREATE TABLE i18n_message (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    namespace     VARCHAR(64) NOT NULL DEFAULT 'common',
    key_name      VARCHAR(255) NOT NULL,
    locale        VARCHAR(16) NOT NULL,
    value_text    TEXT        NOT NULL,
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ns_key_locale (namespace, key_name, locale),
    KEY idx_locale (locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='i18n 词条';

-- 预置 common + biz_error 词条
INSERT INTO i18n_message (namespace, key_name, locale, value_text) VALUES
('common', 'success', 'zh-CN', '操作成功'),
('common', 'success', 'en-US', 'Success'),
('common', 'failed', 'zh-CN', '操作失败'),
('common', 'failed', 'en-US', 'Failed'),
('common', 'unauthorized', 'zh-CN', '未授权访问'),
('common', 'unauthorized', 'en-US', 'Unauthorized'),
('common', 'forbidden', 'zh-CN', '无权访问'),
('common', 'forbidden', 'en-US', 'Forbidden'),
('common', 'not_found', 'zh-CN', '资源不存在'),
('common', 'not_found', 'en-US', 'Not Found'),
('common', 'rate_limited', 'zh-CN', '请求过于频繁'),
('common', 'rate_limited', 'en-US', 'Rate Limited'),
('common', 'saved', 'zh-CN', '保存成功'),
('common', 'saved', 'en-US', 'Saved'),
('common', 'deleted', 'zh-CN', '删除成功'),
('common', 'deleted', 'en-US', 'Deleted'),
('biz_error', 'invalid_param', 'zh-CN', '参数错误：{0}'),
('biz_error', 'invalid_param', 'en-US', 'Invalid parameter: {0}'),
('biz_error', 'business_failed', 'zh-CN', '业务失败：{0}'),
('biz_error', 'business_failed', 'en-US', 'Business failed: {0}');
