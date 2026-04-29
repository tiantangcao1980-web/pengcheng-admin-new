-- =====================================================================
-- V75: OnlyOffice 在线编辑集成（M1 — V1.0 收口最后一项）
-- =====================================================================

-- 给 sys_doc 增加 OnlyOffice 编辑状态字段（如果存在则跳过）
ALTER TABLE sys_doc
    ADD COLUMN IF NOT EXISTS file_type      VARCHAR(16)  COMMENT 'docx/xlsx/pptx/txt/md',
    ADD COLUMN IF NOT EXISTS oo_doc_key     VARCHAR(64)  COMMENT 'OnlyOffice 编辑会话 key（防并发覆盖）',
    ADD COLUMN IF NOT EXISTS oo_last_save   DATETIME     COMMENT '最近 OnlyOffice 保存时间',
    ADD COLUMN IF NOT EXISTS oo_last_user   BIGINT       COMMENT '最近编辑者 user_id';

-- OnlyOffice 编辑会话日志（审计 + 调试）
CREATE TABLE onlyoffice_session_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    doc_id        BIGINT      NOT NULL,
    doc_key       VARCHAR(64) NOT NULL,
    event         VARCHAR(32) NOT NULL COMMENT 'OPEN/SAVE/SAVE_CORRUPTED/CLOSE/USER_JOIN/USER_LEAVE',
    user_id       BIGINT,
    user_name     VARCHAR(64),
    status        TINYINT     COMMENT 'OnlyOffice status code 0/1/2/3/4/6/7',
    file_url      VARCHAR(512),
    error_msg     VARCHAR(512),
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_doc_time (doc_id, create_time),
    KEY idx_event (event, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OnlyOffice 会话日志';
