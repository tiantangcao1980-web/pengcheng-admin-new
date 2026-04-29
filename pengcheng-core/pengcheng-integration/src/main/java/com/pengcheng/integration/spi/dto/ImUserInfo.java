package com.pengcheng.integration.spi.dto;

import lombok.Data;

import java.util.List;

/**
 * IM 平台返回的外部用户信息（SSO 回调解析结果）。
 */
@Data
public class ImUserInfo {

    /** Provider 标识 */
    private String provider;

    /** 外部平台用户 ID（企业微信 userId / 钉钉 userId / 飞书 open_id） */
    private String externalId;

    /** 用户姓名 */
    private String name;

    /** 头像 URL */
    private String avatar;

    /** 手机号（部分平台可能为空） */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 所属外部部门 ID 列表 */
    private List<String> externalDeptIds;
}
