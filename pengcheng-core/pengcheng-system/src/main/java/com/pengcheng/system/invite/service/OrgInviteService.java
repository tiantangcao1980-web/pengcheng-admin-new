package com.pengcheng.system.invite.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pengcheng.system.invite.entity.OrgInvite;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织邀请服务
 */
public interface OrgInviteService extends IService<OrgInvite> {

    /**
     * 创建邀请（默认链接渠道）
     */
    OrgInvite createInvite(String email, String phone, List<Long> roleIds, Long deptId, LocalDateTime expiresAt);

    /**
     * 邀请列表
     */
    List<OrgInvite> listInvites(Integer status);

    /**
     * 撤销邀请
     */
    void revokeInvite(Long id);

    /**
     * 接受邀请
     */
    OrgInvite acceptInvite(String code, Long userId);

    // ===================== channel-aware 方法 =====================

    /**
     * 短信渠道邀请：创建邀请并触发短信发送
     *
     * @param phone     被邀请手机号（必填）
     * @param roleIds   角色 ID 列表
     * @param deptId    部门 ID
     * @param expiresAt 过期时间，null 表示默认 7 天
     * @param tenantId  租户 ID，null 表示当前组织
     * @return 已持久化的邀请记录
     */
    OrgInvite sendSms(String phone, List<Long> roleIds, Long deptId, LocalDateTime expiresAt, Long tenantId);

    /**
     * 链接渠道邀请：创建邀请并记录链接（可由前端自行拼接分享）
     *
     * @param email     被邀请邮箱（可选）
     * @param phone     被邀请手机号（可选，email/phone 至少一项）
     * @param roleIds   角色 ID 列表
     * @param deptId    部门 ID
     * @param expiresAt 过期时间，null 表示默认 7 天
     * @param tenantId  租户 ID，null 表示当前组织
     * @return 已持久化的邀请记录
     */
    OrgInvite sendLink(String email, String phone, List<Long> roleIds, Long deptId, LocalDateTime expiresAt, Long tenantId);

    /**
     * 二维码渠道邀请：创建邀请并生成二维码图片上传至 OSS，URL 写回 qrcodeUrl
     *
     * @param roleIds   角色 ID 列表
     * @param deptId    部门 ID
     * @param expiresAt 过期时间，null 表示默认 7 天
     * @param tenantId  租户 ID，null 表示当前组织
     * @return 已持久化且含 qrcodeUrl 的邀请记录
     */
    OrgInvite sendQrcode(List<Long> roleIds, Long deptId, LocalDateTime expiresAt, Long tenantId);

    /**
     * Excel 批量导入邀请：逐条创建邀请并共享同一 excelBatchId
     *
     * @param rows      每行包含 email/phone/roleIds/deptId 信息的列表
     * @param expiresAt 过期时间，null 表示默认 7 天
     * @param tenantId  租户 ID，null 表示当前组织
     * @return 已创建的邀请记录列表
     */
    List<OrgInvite> batchImportFromExcel(List<ExcelInviteRow> rows, LocalDateTime expiresAt, Long tenantId);

    /**
     * Excel 批量导入行
     */
    class ExcelInviteRow {
        private String email;
        private String phone;
        private List<Long> roleIds;
        private Long deptId;

        public ExcelInviteRow() {
        }

        public ExcelInviteRow(String email, String phone, List<Long> roleIds, Long deptId) {
            this.email = email;
            this.phone = phone;
            this.roleIds = roleIds;
            this.deptId = deptId;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public List<Long> getRoleIds() { return roleIds; }
        public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
        public Long getDeptId() { return deptId; }
        public void setDeptId(Long deptId) { this.deptId = deptId; }
    }
}
