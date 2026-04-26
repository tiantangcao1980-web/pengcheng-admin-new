package com.pengcheng.auth.invite;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.common.result.Result;
import com.pengcheng.sms.SmsServiceFactory;
import com.pengcheng.system.tenant.dto.InviteCreateRequest;
import com.pengcheng.system.tenant.dto.InviteImportResult;
import com.pengcheng.system.tenant.entity.TenantMemberInvite;
import com.pengcheng.system.tenant.service.TenantMemberInviteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 租户成员邀请 Controller（多渠道）。
 *
 * <p>路由：/auth/tenant/invite/*。短信渠道复用 {@link SmsServiceFactory}。
 */
@Slf4j
@RestController
@RequestMapping("/auth/tenant/invite")
@RequiredArgsConstructor
public class TenantInviteController {

    private final TenantMemberInviteService inviteService;
    private final SmsServiceFactory smsServiceFactory;

    /**
     * 创建邀请（SMS / LINK / QRCODE 通用入口）
     */
    @PostMapping
    public Result<TenantMemberInvite> create(@RequestBody InviteCreateRequest request) {
        Long inviterId = currentUserId();
        TenantMemberInvite invite = inviteService.createInvite(request, inviterId);

        // SMS 渠道：复用 SmsServiceFactory 触发短信
        if (TenantMemberInvite.CHANNEL_SMS.equals(invite.getChannel())) {
            try {
                smsServiceFactory.sendCode(invite.getPhone(), "邀请码 " + invite.getInviteCode());
            } catch (Exception e) {
                log.warn("发送邀请短信失败：phone={}", invite.getPhone(), e);
            }
        }
        return Result.ok(invite);
    }

    /**
     * Excel/CSV 批量导入。Content-Type: multipart/form-data
     */
    @PostMapping("/import")
    public Result<InviteImportResult> importInvites(@RequestParam("tenantId") Long tenantId,
                                                    @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 CSV 文件");
        }
        try {
            InviteImportResult result = inviteService.importInvites(tenantId, file.getInputStream(), currentUserId());
            return Result.ok(result);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败：" + e.getMessage());
        }
    }

    /**
     * 列表
     */
    @GetMapping
    public Result<List<TenantMemberInvite>> list(@RequestParam Long tenantId,
                                                 @RequestParam(required = false) Integer status) {
        return Result.ok(inviteService.listInvites(tenantId, status));
    }

    /**
     * 撤销
     */
    @PostMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        inviteService.revokeInvite(id, currentUserId());
        return Result.ok();
    }

    /**
     * 用 code 查询邀请（用于二维码扫码 / 链接落地校验，无需登录态）
     */
    @GetMapping("/by-code/{code}")
    public Result<TenantMemberInvite> getByCode(@PathVariable String code) {
        return Result.ok(inviteService.getByCode(code));
    }

    /**
     * 接受邀请（已登录用户操作）
     */
    @PostMapping("/accept")
    public Result<TenantMemberInvite> accept(@RequestBody AcceptRequest req) {
        return Result.ok(inviteService.acceptInvite(req.getCode(), currentUserId()));
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("请先登录");
        }
        return StpUtil.getLoginIdAsLong();
    }

    @Data
    public static class AcceptRequest {
        private String code;
    }
}
