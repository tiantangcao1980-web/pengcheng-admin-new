package com.pengcheng.message.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户三通道画像
 *
 * <p>由 {@link UserChannelResolver} 从用户表/小程序授权表/心跳表/绑定表组合得到。
 * 仅作为 ChannelPushService 的内部 DTO 使用。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChannelProfile {

    private Long userId;

    /** APP 注册 ID（极光 / 友盟 / 个推） */
    private String appRegistrationId;

    /** APP 是否在线 */
    private boolean appOnline;

    /** 小程序 OPENID */
    private String miniProgramOpenId;

    /** 是否拥有有效订阅 */
    private boolean miniProgramSubscribed;

    /** Web 站内信是否启用（默认 true） */
    @Builder.Default
    private boolean webInboxEnabled = true;
}
