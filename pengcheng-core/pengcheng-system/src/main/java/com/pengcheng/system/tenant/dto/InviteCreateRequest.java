package com.pengcheng.system.tenant.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建单条成员邀请请求
 */
@Data
public class InviteCreateRequest {

    /** 租户ID */
    private Long tenantId;

    /** 渠道 SMS / LINK / QRCODE */
    private String channel;

    /** 被邀请手机号（SMS 必填） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 预设部门 */
    private Long deptId;

    /** 角色 ID 列表 */
    private List<Long> roleIds;

    /** 过期小时数（默认 72） */
    private Integer expireHours;
}
