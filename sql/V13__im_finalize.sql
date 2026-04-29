-- ----------------------------
-- V13: IM 收尾（V1.0 Sprint B 第 7 任务）
-- 1) 补 sys_chat_message 字段（与 entity 对齐 + 添加 5 条设计纪律所需字段）
-- 2) 新建 im_conversation 会话表（避免每次扫消息表算"最近联系人/未读数"）
-- ----------------------------

-- 1. sys_chat_message 字段补齐
ALTER TABLE `sys_chat_message`
  ADD COLUMN `conversation_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '会话ID（min(uid,uid)_max(uid,uid) 或 group_xxx）' AFTER `receiver_id`,
  ADD COLUMN `seq` BIGINT NULL DEFAULT NULL COMMENT '全局递增序号（离线拉取断点用）' AFTER `msg_type`,
  ADD COLUMN `recalled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否撤回：0否 1是' AFTER `seq`,
  ADD COLUMN `reply_to_id` BIGINT NULL DEFAULT NULL COMMENT '引用回复的消息ID' AFTER `recalled`,
  ADD COLUMN `delivered` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已投递：0否 1是' AFTER `reply_to_id`,
  ADD COLUMN `read_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '已读标记（与 is_read 双轨保持兼容；新代码使用 read_flag）' AFTER `delivered`,
  ADD COLUMN `priority` TINYINT NOT NULL DEFAULT 0 COMMENT '消息优先级：0普通 1重要 2紧急' AFTER `read_flag`,
  ADD COLUMN `pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否 1是' AFTER `priority`,
  ADD COLUMN `extra` JSON NULL DEFAULT NULL COMMENT '扩展字段（业务消息类型元数据）' AFTER `pinned`,
  ADD INDEX `idx_chat_conversation` (`conversation_id`) USING BTREE,
  ADD INDEX `idx_chat_seq` (`seq`) USING BTREE;

-- 旧数据兼容：把 is_read 同步到 read_flag
UPDATE `sys_chat_message` SET `read_flag` = `is_read` WHERE `read_flag` = 0 AND `is_read` = 1;

-- 2. 会话表（一行一会话，避免每次扫消息表算"最近联系人/未读"）
CREATE TABLE IF NOT EXISTS `im_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `owner_id` BIGINT NOT NULL COMMENT '会话归属用户（即一会话两行：A 和 B 各一行）',
  `peer_id` BIGINT NOT NULL COMMENT '对端用户ID（单聊场景）',
  `peer_type` TINYINT NOT NULL DEFAULT 1 COMMENT '对端类型：1单聊 2群聊',
  `last_msg_id` BIGINT NULL DEFAULT NULL COMMENT '最近消息ID',
  `last_msg_content` VARCHAR(500) NULL DEFAULT NULL COMMENT '最近消息内容（前缀）',
  `last_msg_time` DATETIME NULL DEFAULT NULL COMMENT '最近消息时间',
  `unread_count` INT NOT NULL DEFAULT 0 COMMENT '未读数（Redis 为权威，DB 为兜底）',
  `pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `muted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否免打扰',
  `extra` JSON NULL DEFAULT NULL COMMENT '扩展',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_conv_owner_conv` (`owner_id`, `conversation_id`) USING BTREE,
  INDEX `idx_conv_owner_time` (`owner_id`, `last_msg_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'IM 会话索引表' ROW_FORMAT = DYNAMIC;
