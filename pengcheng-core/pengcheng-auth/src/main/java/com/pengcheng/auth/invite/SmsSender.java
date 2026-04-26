package com.pengcheng.auth.invite;

import com.pengcheng.sms.SmsService;
import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.sender.InviteChannelSender;
import com.pengcheng.system.invite.support.InviteChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 短信邀请渠道 Sender。
 *
 * <p>向被邀请人手机号发送包含邀请码的短信通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSender implements InviteChannelSender {

    private final SmsService smsService;

    @Override
    public String channel() {
        return InviteChannel.SMS;
    }

    @Override
    public void send(OrgInvite invite) {
        String phone = invite.getPhone();
        if (!StringUtils.hasText(phone)) {
            log.warn("[SmsSender] 邀请 {} 未配置手机号，跳过短信发送", invite.getId());
            return;
        }
        String code = invite.getInviteCode();
        boolean success = smsService.sendCode(phone, code);
        if (success) {
            log.info("[SmsSender] 邀请短信发送成功: phone={}, inviteCode={}", phone, code);
        } else {
            log.warn("[SmsSender] 邀请短信发送失败: phone={}, inviteCode={}", phone, code);
        }
    }
}
