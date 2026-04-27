-- V65 会议室资源 + 预订 + 签到 + AI 纪要 + 行动项
-- Phase 4 J5: MeetingRoom / MeetingBooking / MeetingAttendance / MeetingMinutesAi / MeetingActionItem

-- 会议室
CREATE TABLE meeting_room (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL,
    location    VARCHAR(255),
    capacity    INT NOT NULL DEFAULT 0,
    facilities  VARCHAR(512) COMMENT '逗号分隔：投影/白板/视频/电话',
    enabled     TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 会议预订（含会议室占用）
CREATE TABLE meeting_booking (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    organizer_id BIGINT NOT NULL,
    room_id      BIGINT,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NOT NULL,
    status       TINYINT NOT NULL DEFAULT 0 COMMENT '0预订 1进行中 2已结束 3取消',
    attendees    TEXT COMMENT 'JSON [userId, ...]',
    online_url   VARCHAR(255) COMMENT '腾讯会议/Zoom 链接',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_room_time (room_id, start_time, end_time),
    KEY idx_organizer (organizer_id, start_time)
);

-- 签到
CREATE TABLE meeting_attendance (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    sign_time  DATETIME NOT NULL,
    sign_type  VARCHAR(16) DEFAULT 'QRCODE' COMMENT 'QRCODE/MANUAL/NFC',
    UNIQUE KEY uk_booking_user (booking_id, user_id)
);

-- AI 纪要（独立表，与 meeting_minutes 老表并存）
CREATE TABLE meeting_minutes_ai (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id  BIGINT NOT NULL,
    audio_url   VARCHAR(512) COMMENT '上传的录音 OSS 路径',
    transcript  LONGTEXT COMMENT 'ASR 全文',
    summary     TEXT COMMENT 'LLM 摘要',
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/TRANSCRIBING/SUMMARIZING/READY/FAILED',
    error_msg   VARCHAR(512),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_booking (booking_id)
);

-- 行动项（自动从纪要提取 → 创建 sys_todo 关联）
CREATE TABLE meeting_action_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id  BIGINT NOT NULL,
    minutes_id  BIGINT NOT NULL,
    content     VARCHAR(512) NOT NULL,
    owner_id    BIGINT,
    due_date    DATE,
    todo_id     BIGINT COMMENT '关联的 sys_todo id（创建后回填）',
    status      TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已完成 2取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_booking (booking_id),
    KEY idx_owner_status (owner_id, status)
);
