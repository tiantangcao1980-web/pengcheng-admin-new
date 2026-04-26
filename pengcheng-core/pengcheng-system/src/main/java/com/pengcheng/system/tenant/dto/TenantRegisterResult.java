package com.pengcheng.system.tenant.dto;

import lombok.Data;

/**
 * 企业注册结果
 */
@Data
public class TenantRegisterResult {

    /** 租户ID */
    private Long tenantId;

    /** 租户编码 */
    private String tenantCode;

    /** 管理员用户ID */
    private Long adminUserId;

    /** 默认部门ID */
    private Long defaultDeptId;

    /** 默认管理员角色ID */
    private Long defaultRoleId;
}
