package com.pengcheng.push.unified;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 统一推送调度的目标描述
 *
 * <p>承载用户态信息：是否拥有 APP / 是否在线 / 是否有小程序 OPENID 等，
 * {@link UnifiedPushDispatcher} 据此选择合适的通道。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTarget {

    /** 接收用户 ID（业务侧主键，字符串以兼容多源） */
    private String userId;

    /** APP 端注册 ID（极光 / 友盟 / 个推 deviceId）；为空表示未装 APP */
    private String registrationId;

    /** APP 是否在线（最近一次心跳/socket 连接 < 阈值） */
    private boolean appOnline;

    /** 微信小程序 OPENID；为空表示未授权小程序 */
    private String miniProgramOpenId;

    /** 是否允许接收订阅消息（用户授权一次性 / 长期订阅） */
    private boolean miniProgramSubscribed;

    /** 是否允许 Web 端站内信（默认始终允许，用于兜底） */
    @Builder.Default
    private boolean webInboxEnabled = true;

    /** 业务自定义扩展（trace / from / 业务 ID 等） */
    private Map<String, String> extras;
}
