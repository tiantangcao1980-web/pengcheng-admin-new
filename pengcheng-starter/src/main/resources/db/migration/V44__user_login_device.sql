-- ========================================================
-- V44__user_login_device.sql
-- V4.0 MVP 闭环 ① 个人中心·设备管理：
--   user_login_device 表 + 单点登录踢下线（基于 Sa-Token tokenValue）
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `user_login_device` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `token_value`   VARCHAR(128) NOT NULL COMMENT 'Sa-Token tokenValue（用于踢下线）',
    `client_type`   VARCHAR(16)  NOT NULL DEFAULT 'WEB' COMMENT '客户端: WEB / ADMIN / APP / MINIAPP',
    `device_id`     VARCHAR(64)  NULL DEFAULT NULL COMMENT '设备唯一标识（前端生成）',
    `device_name`   VARCHAR(128) NULL DEFAULT NULL COMMENT '设备名称：如 iPhone 15 / Chrome on macOS',
    `os`            VARCHAR(64)  NULL DEFAULT NULL COMMENT '操作系统',
    `browser`       VARCHAR(64)  NULL DEFAULT NULL COMMENT '浏览器/App',
    `ip`            VARCHAR(64)  NULL DEFAULT NULL COMMENT '登录IP',
    `location`      VARCHAR(128) NULL DEFAULT NULL COMMENT '登录归属地',
    `login_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `last_active`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近活跃时间',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-已下线 1-在线 2-被踢下线',
    `create_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_login_device_token` (`token_value`) USING BTREE,
    INDEX `idx_user_login_device_user` (`user_id`, `status`) USING BTREE,
    INDEX `idx_user_login_device_login_time` (`login_time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户登录设备表'
  ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
