package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.RepeatSubmit;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.invite.entity.OrgInvite;
import com.pengcheng.system.invite.service.OrgInviteService;
import com.pengcheng.system.invite.service.OrgInviteService.ExcelInviteRow;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织邀请多渠道控制器（E4 新增）。
 *
 * <p>提供短信、链接、二维码、Excel 四种渠道的邀请发送端点：
 * <ul>
 *   <li>POST /admin/org/invite/sms</li>
 *   <li>POST /admin/org/invite/link</li>
 *   <li>POST /admin/org/invite/qrcode</li>
 *   <li>POST /admin/org/invite/batch-excel</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/org/invite")
@RequiredArgsConstructor
public class OrgInviteChannelController {

    private final OrgInviteService orgInviteService;

    /**
     * 短信渠道邀请
     */
    @PostMapping("/sms")
    @SaCheckPermission("sys:dept:edit")
    @RepeatSubmit
    @Log(title = "短信邀请", businessType = BusinessType.INSERT)
    public Result<OrgInvite> sendSms(@RequestBody SmsInviteRequest request) {
        return Result.ok(orgInviteService.sendSms(
                request.getPhone(),
                request.getRoleIds(),
                request.getDeptId(),
                request.getExpiresAt(),
                request.getTenantId()
        ));
    }

    /**
     * 链接渠道邀请
     */
    @PostMapping("/link")
    @SaCheckPermission("sys:dept:edit")
    @RepeatSubmit
    @Log(title = "链接邀请", businessType = BusinessType.INSERT)
    public Result<OrgInvite> sendLink(@RequestBody LinkInviteRequest request) {
        return Result.ok(orgInviteService.sendLink(
                request.getEmail(),
                request.getPhone(),
                request.getRoleIds(),
                request.getDeptId(),
                request.getExpiresAt(),
                request.getTenantId()
        ));
    }

    /**
     * 二维码渠道邀请
     */
    @PostMapping("/qrcode")
    @SaCheckPermission("sys:dept:edit")
    @RepeatSubmit
    @Log(title = "二维码邀请", businessType = BusinessType.INSERT)
    public Result<OrgInvite> sendQrcode(@RequestBody QrcodeInviteRequest request) {
        return Result.ok(orgInviteService.sendQrcode(
                request.getRoleIds(),
                request.getDeptId(),
                request.getExpiresAt(),
                request.getTenantId()
        ));
    }

    /**
     * Excel 批量导入邀请
     */
    @PostMapping("/batch-excel")
    @SaCheckPermission("sys:dept:edit")
    @RepeatSubmit
    @Log(title = "Excel批量邀请", businessType = BusinessType.INSERT)
    public Result<List<OrgInvite>> batchExcel(@RequestBody BatchExcelInviteRequest request) {
        return Result.ok(orgInviteService.batchImportFromExcel(
                request.getRows(),
                request.getExpiresAt(),
                request.getTenantId()
        ));
    }

    // ===================== Request DTO =====================

    @Data
    public static class SmsInviteRequest {
        private String phone;
        private List<Long> roleIds;
        private Long deptId;
        private LocalDateTime expiresAt;
        private Long tenantId;
    }

    @Data
    public static class LinkInviteRequest {
        private String email;
        private String phone;
        private List<Long> roleIds;
        private Long deptId;
        private LocalDateTime expiresAt;
        private Long tenantId;
    }

    @Data
    public static class QrcodeInviteRequest {
        private List<Long> roleIds;
        private Long deptId;
        private LocalDateTime expiresAt;
        private Long tenantId;
    }

    @Data
    public static class BatchExcelInviteRequest {
        private List<ExcelInviteRow> rows;
        private LocalDateTime expiresAt;
        private Long tenantId;
    }
}
