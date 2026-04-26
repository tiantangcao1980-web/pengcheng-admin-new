package com.pengcheng.system.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pengcheng.system.tenant.dto.InviteCreateRequest;
import com.pengcheng.system.tenant.dto.InviteImportResult;
import com.pengcheng.system.tenant.entity.TenantMemberInvite;

import java.io.InputStream;
import java.util.List;

/**
 * 租户成员邀请服务（多渠道：短信 / 链接 / 二维码 / Excel/CSV 批量导入）
 */
public interface TenantMemberInviteService extends IService<TenantMemberInvite> {

    /**
     * 创建一条邀请（SMS/LINK/QRCODE 通用入口）。
     * <ul>
     *   <li>SMS：必须提供 phone，并自动通过 SmsServiceFactory 发送邀请短信</li>
     *   <li>LINK / QRCODE：不强制 phone，落库即可，前端构造短链 / 二维码</li>
     * </ul>
     */
    TenantMemberInvite createInvite(InviteCreateRequest request, Long inviterId);

    /**
     * Excel/CSV 批量导入。CSV 格式（首行为表头，可被忽略）：
     * <pre>phone,deptName,roleCode</pre>
     * 失败的行会回写 fail_reason 但不会落库 invite。
     */
    InviteImportResult importInvites(Long tenantId, InputStream csv, Long inviterId);

    /**
     * 列表查询（按 tenantId + 状态可选）
     */
    List<TenantMemberInvite> listInvites(Long tenantId, Integer status);

    /**
     * 撤销
     */
    void revokeInvite(Long id, Long operatorId);

    /**
     * 接受邀请。validate 过期 / 状态后将 acceptedUserId / acceptedAt 写入。
     */
    TenantMemberInvite acceptInvite(String code, Long acceptUserId);

    /**
     * 通过 code 查询（用于二维码扫码 / 链接落地校验）
     */
    TenantMemberInvite getByCode(String code);
}
