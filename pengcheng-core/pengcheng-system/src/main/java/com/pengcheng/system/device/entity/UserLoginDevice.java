package com.pengcheng.system.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户登录设备
 *
 * <p>每次登录后由 LoginHelper 触发回调写入；提供"我的设备"列表 + 踢下线能力。
 */
@Data
@TableName("user_login_device")
public class UserLoginDevice implements Serializable {

    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_KICKED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** Sa-Token tokenValue（用于 kickoutByTokenValue） */
    private String tokenValue;

    /** 客户端类型 WEB / ADMIN / APP / MINIAPP */
    private String clientType;

    /** 设备唯一标识 */
    private String deviceId;

    /** 设备名 */
    private String deviceName;

    /** 操作系统 */
    private String os;

    /** 浏览器或 App */
    private String browser;

    /** 登录IP */
    private String ip;

    /** 归属地 */
    private String location;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 最近活跃 */
    private LocalDateTime lastActive;

    /** 状态 */
    private Integer status;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
