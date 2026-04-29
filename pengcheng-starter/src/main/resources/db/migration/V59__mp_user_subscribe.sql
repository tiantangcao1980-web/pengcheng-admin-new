-- V59: 小程序订阅消息授权记录表
-- 记录用户通过 wx.requestSubscribeMessage 授权的订阅信息

CREATE TABLE mp_user_subscribe (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL                        COMMENT '系统用户 ID',
    open_id             VARCHAR(64)  NOT NULL                        COMMENT '微信小程序 openId',
    template_id         VARCHAR(64)  NOT NULL                        COMMENT '订阅消息模板 ID',
    quota               INT          NOT NULL DEFAULT 1              COMMENT '用户授权次数（累计）',
    used                INT          NOT NULL DEFAULT 0              COMMENT '已消费次数',
    last_subscribe_time DATETIME     NOT NULL                        COMMENT '最近一次授权时间',
    revoked             TINYINT      NOT NULL DEFAULT 0              COMMENT '是否已撤销（用户在小程序设置中关闭）：0-否 1-是',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_user_template (user_id, template_id),
    KEY idx_open_id     (open_id),
    KEY idx_quota       (revoked, quota, used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序订阅消息用户授权记录';
