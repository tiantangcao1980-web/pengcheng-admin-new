-- V43: 班次模板表（V4.0 闭环②，D2 任务）
-- 注意：原任务文档要求 V12/V13/V14，但 V12/V14 已分别用于 document_space / memory_system，
--       为避免 Flyway 校验冲突，版本号顺延到 V43/V44/V45。

CREATE TABLE IF NOT EXISTS attendance_shift (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_name VARCHAR(64) NOT NULL COMMENT '班次名称',
    shift_type TINYINT NOT NULL COMMENT '班次类型 1=固定 2=跨夜 3=弹性',
    start_time TIME NULL COMMENT '上班时间',
    end_time TIME NULL COMMENT '下班时间',
    late_grace_minutes INT NULL DEFAULT 0 COMMENT '迟到容忍分钟',
    early_grace_minutes INT NULL DEFAULT 0 COMMENT '早退容忍分钟',
    min_work_minutes INT NULL COMMENT '弹性班次最低工作分钟数',
    remark VARCHAR(255) NULL COMMENT '备注',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0-未删除 1-已删除',
    KEY idx_enabled (enabled),
    KEY idx_shift_type (shift_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班次模板';

-- 内置 4 种班次模板（idempotent）
INSERT INTO attendance_shift (shift_name, shift_type, start_time, end_time, late_grace_minutes, early_grace_minutes, min_work_minutes, remark, enabled, create_time, update_time)
SELECT '标准班', 1, '09:00:00', '18:00:00', 5, 5, NULL, '默认 9-18 标准班', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM attendance_shift WHERE shift_name = '标准班');

INSERT INTO attendance_shift (shift_name, shift_type, start_time, end_time, late_grace_minutes, early_grace_minutes, min_work_minutes, remark, enabled, create_time, update_time)
SELECT '早班', 1, '07:00:00', '15:00:00', 0, 0, NULL, '工厂/物业早班', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM attendance_shift WHERE shift_name = '早班');

INSERT INTO attendance_shift (shift_name, shift_type, start_time, end_time, late_grace_minutes, early_grace_minutes, min_work_minutes, remark, enabled, create_time, update_time)
SELECT '夜班', 2, '22:00:00', '06:00:00', 0, 0, NULL, '跨夜班次 22:00 → 次日 06:00', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM attendance_shift WHERE shift_name = '夜班');

INSERT INTO attendance_shift (shift_name, shift_type, start_time, end_time, late_grace_minutes, early_grace_minutes, min_work_minutes, remark, enabled, create_time, update_time)
SELECT '弹性班', 3, NULL, NULL, 0, 0, 480, '弹性班次满足 8 小时即可', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM attendance_shift WHERE shift_name = '弹性班');
