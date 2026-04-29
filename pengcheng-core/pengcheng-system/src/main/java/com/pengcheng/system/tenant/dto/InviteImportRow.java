package com.pengcheng.system.tenant.dto;

import lombok.Data;

/**
 * Excel/CSV 批量导入单行结果
 */
@Data
public class InviteImportRow {

    /** 行号（1-based, 含表头） */
    private Integer lineNo;

    /** 手机号 */
    private String phone;

    /** 部门名 */
    private String deptName;

    /** 角色编码 */
    private String roleCode;

    /** 是否成功 */
    private Boolean success;

    /** 失败原因 */
    private String failReason;

    /** 关联的邀请 ID（如果 success） */
    private Long inviteId;

    public static InviteImportRow ok(int lineNo, String phone, Long inviteId) {
        InviteImportRow r = new InviteImportRow();
        r.setLineNo(lineNo);
        r.setPhone(phone);
        r.setSuccess(true);
        r.setInviteId(inviteId);
        return r;
    }

    public static InviteImportRow fail(int lineNo, String phone, String reason) {
        InviteImportRow r = new InviteImportRow();
        r.setLineNo(lineNo);
        r.setPhone(phone);
        r.setSuccess(false);
        r.setFailReason(reason);
        return r;
    }
}
