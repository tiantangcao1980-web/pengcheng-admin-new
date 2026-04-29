package com.pengcheng.system.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户/企业实体
 *
 * <p>V4.0 MVP 闭环 ① 账户与组织：企业一分钟开通的核心载体。
 * 一个 tenant 对应一个企业管理员（admin_user_id）和一个默认部门。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant")
public class Tenant extends BaseEntity {

    /** 企业名称 */
    private String name;

    /** 企业编码（用于短链/二维码） */
    private String code;

    /** 行业 */
    private String industry;

    /** 规模 */
    private String scale;

    /** 管理员用户ID */
    private Long adminUserId;

    /** 默认部门ID */
    private Long defaultDeptId;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 试用/到期时间 */
    private LocalDateTime expireAt;

    /** 备注 */
    private String remark;
}
