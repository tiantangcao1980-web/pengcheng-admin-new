package com.pengcheng.auth.invite;

import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.sender.InviteChannelSender;
import com.pengcheng.system.invite.support.InviteChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 链接邀请渠道 Sender。
 *
 * <p>生成并返回可分享的邀请链接（日志记录/事件通知）。
 * 实际项目可在此推送至消息中心或邮件。
 */
@Slf4j
@Component
public class LinkSender implements InviteChannelSender {

    @Override
    public String channel() {
        return InviteChannel.LINK;
    }

    @Override
    public void send(OrgInvite invite) {
        // 链接渠道：邀请码已在 OrgInvite 中记录，前端自行拼接分享链接
        log.info("[LinkSender] 邀请链接已就绪: inviteCode={}, expiresAt={}",
                invite.getInviteCode(), invite.getExpiresAt());
    }
}
