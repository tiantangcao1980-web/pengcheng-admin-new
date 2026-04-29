package com.pengcheng.system.invite.sender;

import com.pengcheng.system.invite.entity.OrgInvite;

/**
 * 邀请渠道发送 SPI。
 *
 * <p>各渠道实现（短信、链接、二维码、Excel）需实现此接口，并声明各自支持的渠道类型。
 * 具体调度由 {@code OrgInviteService} 的 channel-aware 方法负责。
 */
public interface InviteChannelSender {

    /**
     * 返回此 Sender 支持的渠道标识，对应 {@code OrgInvite#channel} 字段值。
     *
     * @return 渠道标识，如 "SMS" / "LINK" / "QRCODE" / "EXCEL"
     */
    String channel();

    /**
     * 执行发送操作。
     *
     * @param invite 已持久化的邀请记录（含 inviteCode、channel 等信息）
     */
    void send(OrgInvite invite);
}
