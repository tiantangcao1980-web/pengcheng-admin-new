-- V44: 补卡申请单（V4.0 闭环②，D2 任务）

CREATE TABLE IF NOT EXISTS attendance_correction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '申请人 sys_user.id',
    correction_date DATE NOT NULL COMMENT '补卡日期',
    correction_type TINYINT NOT NULL COMMENT '补卡类型 1=上班 2=下班',
    expected_time DATETIME NOT NULL COMMENT '应该的打卡时间',
    reason VARCHAR(500) NULL COMMENT '补卡原因',
    approval_instance_id BIGINT NULL COMMENT '关联流程实例 ID（approval_instance.id）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=待审批 2=已通过 3=已驳回',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0-未删除 1-已删除',
    KEY idx_user_date (user_id, correction_date),
    KEY idx_status (status),
    KEY idx_approval_instance (approval_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补卡申请单';
