package com.pengcheng.auth.invite;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.pengcheng.oss.FileStorage;
import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.sender.InviteChannelSender;
import com.pengcheng.system.invite.support.InviteChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 二维码邀请渠道 Sender。
 *
 * <p>使用 Hutool QrCode 工具生成邀请二维码，上传至 OSS 并将 URL 写回
 * {@code OrgInvite#qrcodeUrl}。调用方需在 send() 后持久化 invite 记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QrcodeSender implements InviteChannelSender {

    private static final int QR_SIZE = 300;
    private static final String QR_PATH_PREFIX = "invite/qrcode/";

    private final FileStorage fileStorage;

    @Override
    public String channel() {
        return InviteChannel.QRCODE;
    }

    @Override
    public void send(OrgInvite invite) {
        String inviteCode = invite.getInviteCode();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            QrCodeUtil.generate(inviteCode, QrConfig.of(QR_SIZE, QR_SIZE), "png", baos);
            String fileName = inviteCode + ".png";
            String url = fileStorage.upload(new ByteArrayInputStream(baos.toByteArray()), QR_PATH_PREFIX, fileName);
            invite.setQrcodeUrl(url);
            log.info("[QrcodeSender] 二维码已生成并上传: inviteCode={}, url={}", inviteCode, url);
        } catch (Exception e) {
            log.warn("[QrcodeSender] 二维码生成失败: inviteCode={}", inviteCode, e);
        }
    }
}
