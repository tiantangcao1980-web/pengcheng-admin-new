package com.pengcheng.auth.invite;

import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.sender.InviteChannelSender;
import com.pengcheng.system.invite.support.InviteChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Excel 批量导入邀请渠道 Sender。
 *
 * <p>Excel 渠道的邀请记录由 {@code OrgInviteService#batchImportFromExcel} 批量创建；
 * 此 Sender 负责在每条记录创建后执行后续通知（如触发异步邮件、记录 batch 日志等）。
 */
@Slf4j
@Component
public class ExcelSender implements InviteChannelSender {

    @Override
    public String channel() {
        return InviteChannel.EXCEL;
    }

    @Override
    public void send(OrgInvite invite) {
        String batchId = invite.getExcelBatchId();
        String target = StringUtils.hasText(invite.getEmail()) ? invite.getEmail()
                : StringUtils.hasText(invite.getPhone()) ? invite.getPhone() : "unknown";
        log.info("[ExcelSender] Excel 批量邀请记录已创建: batchId={}, target={}, inviteCode={}",
                batchId, target, invite.getInviteCode());
        // Phase 6: 可在此触发异步邮件/通知
    }
}
