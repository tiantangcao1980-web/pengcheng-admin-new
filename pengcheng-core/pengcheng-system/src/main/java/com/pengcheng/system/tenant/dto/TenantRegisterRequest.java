package com.pengcheng.system.tenant.dto;

import lombok.Data;

/**
 * 企业注册请求（一分钟开通）
 */
@Data
public class TenantRegisterRequest {

    /** 企业名称 */
    private String tenantName;

    /** 行业 */
    private String industry;

    /** 规模 */
    private String scale;

    /** 管理员用户名 */
    private String adminUsername;

    /** 管理员密码 */
    private String adminPassword;

    /** 管理员昵称 */
    private String adminNickname;

    /** 管理员手机号 */
    private String adminPhone;

    /** 管理员邮箱 */
    private String adminEmail;

    /** 短信验证码（可选） */
    private String smsCode;
}
