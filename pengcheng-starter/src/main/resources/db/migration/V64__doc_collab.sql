-- V64 文档实时协作：Y.js 状态 / 评论 / 分享权限

-- Y.js 文档状态快照（增量更新由 ws 同步，定期落库）
CREATE TABLE sys_doc_collab_state (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id       BIGINT NOT NULL,
    state_vector LONGBLOB COMMENT 'Y.js stateVector binary',
    update_blob  LONGBLOB COMMENT 'Y.js update binary（合并后的最新状态）',
    version      BIGINT NOT NULL DEFAULT 0 COMMENT '快照版本号，每次落库 +1',
    last_updater BIGINT,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc (doc_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档 Y.js 协作状态快照';

-- 评论
CREATE TABLE sys_doc_comment (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id      BIGINT NOT NULL,
    parent_id   BIGINT COMMENT '回复目标',
    user_id     BIGINT NOT NULL,
    user_name   VARCHAR(64) NOT NULL,
    content     TEXT NOT NULL,
    anchor_path VARCHAR(255) COMMENT 'Y.js relative position 序列化（评论锚点）',
    mentions    VARCHAR(255) COMMENT '逗号分隔被 @ 的 user_id 列表',
    resolved    TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_doc_time (doc_id, create_time),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档评论（含 @ 和锚点）';

-- 文档分享（权限）
CREATE TABLE sys_doc_share (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id      BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL COMMENT 'USER/DEPT/LINK',
    target_id   BIGINT COMMENT 'target_type=LINK 时为空',
    permission  VARCHAR(16) NOT NULL COMMENT 'READ/COMMENT/EDIT',
    share_code  VARCHAR(32) COMMENT 'target_type=LINK 时的访问码',
    expires_at  DATETIME COMMENT '链接过期',
    create_by   BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_doc (doc_id, target_type),
    UNIQUE KEY uk_share_code (share_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分享权限';
