package com.pengcheng.message.subscribe.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序订阅消息用户授权记录实体
 *
 * <p>每行代表一个用户对一个模板的订阅状态。
 * {@code quota - used > 0 && revoked = 0} 时才允许推送。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mp_user_subscribe")
public class MpUserSubscribe implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 系统用户 ID */
    private Long userId;

    /** 微信小程序 openId */
    private String openId;

    /** 订阅消息模板 ID */
    private String templateId;

    /** 用户授权次数（累计，每次 wx.requestSubscribeMessage 成功 +1） */
    private Integer quota;

    /** 已消费次数（每次实际推送 +1） */
    private Integer used;

    /** 最近一次授权时间 */
    private LocalDateTime lastSubscribeTime;

    /**
     * 是否已撤销：0-否，1-是
     * <p>用户在小程序"订阅消息管理"中关闭时置 1。</p>
     */
    private Integer revoked;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
