ALTER TABLE `sys_calendar_event`
    ADD COLUMN IF NOT EXISTS `meeting_type` tinyint DEFAULT 1 COMMENT '会议类型：1-普通会议 2-视频会 3-电话会';

ALTER TABLE `sys_calendar_event`
    ADD COLUMN IF NOT EXISTS `meeting_url` varchar(500) DEFAULT NULL COMMENT '视频会议链接';

ALTER TABLE `sys_calendar_event`
    ADD COLUMN IF NOT EXISTS `organizer_id` bigint DEFAULT NULL COMMENT '组织者 ID';

INSERT INTO `sys_config_group` (`group_code`, `group_name`, `config_value`, `sort`, `status`, `remark`, `create_time`, `update_time`)
SELECT 'meetingConfig',
       '会议配置',
       '{"defaultReminder":15,"internalNotification":true,"email":false}',
       18,
       1,
       '会议日历提醒配置',
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = 'meetingConfig');
