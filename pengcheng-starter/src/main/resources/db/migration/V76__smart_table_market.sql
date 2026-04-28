-- =====================================================================
-- V76: 智能表格模板市场（M4 — V1.2 长期收口最后一项）
-- =====================================================================

-- 给 smart_table_template 增加市场化字段
ALTER TABLE smart_table_template
    ADD COLUMN IF NOT EXISTS author_user_id  BIGINT       COMMENT '作者用户 ID（built_in=1 时为 NULL）',
    ADD COLUMN IF NOT EXISTS author_name     VARCHAR(64)  COMMENT '作者展示名',
    ADD COLUMN IF NOT EXISTS share_status    VARCHAR(16)  NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/REVIEWING/PUBLIC/REJECTED',
    ADD COLUMN IF NOT EXISTS download_count  INT          NOT NULL DEFAULT 0 COMMENT '被复用次数',
    ADD COLUMN IF NOT EXISTS rating_count    INT          NOT NULL DEFAULT 0 COMMENT '评分人数',
    ADD COLUMN IF NOT EXISTS rating_sum      INT          NOT NULL DEFAULT 0 COMMENT '评分总和（用于算 avg）',
    ADD COLUMN IF NOT EXISTS tags            VARCHAR(255) COMMENT '逗号分隔标签：销售/运营/合同/...',
    ADD COLUMN IF NOT EXISTS cover_url       VARCHAR(512) COMMENT '封面图 URL';

-- 已有内置模板设为 PUBLIC 直接进入市场
UPDATE smart_table_template SET share_status = 'PUBLIC' WHERE built_in = 1;

-- 增加市场索引
ALTER TABLE smart_table_template
    ADD INDEX IF NOT EXISTS idx_share_category (share_status, category),
    ADD INDEX IF NOT EXISTS idx_download_count (download_count DESC);

-- 评分明细
CREATE TABLE smart_table_template_rating (
    id            BIGINT     NOT NULL AUTO_INCREMENT,
    template_id   BIGINT     NOT NULL,
    user_id       BIGINT     NOT NULL,
    rating        TINYINT    NOT NULL COMMENT '1-5 分',
    review        VARCHAR(500) COMMENT '可选短评',
    create_time   DATETIME   DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_user (template_id, user_id),
    KEY idx_template (template_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能表格模板评分';

-- 模板下载记录（用于 download_count 准确累加 + 谁下载了什么）
CREATE TABLE smart_table_template_download (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    template_id   BIGINT   NOT NULL,
    user_id       BIGINT   NOT NULL,
    target_table_id BIGINT COMMENT '生成的目标表 ID（用户从模板创建表后回填）',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_template_time (template_id, create_time),
    KEY idx_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能表格模板下载记录';
