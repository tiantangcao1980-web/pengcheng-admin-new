package com.pengcheng.system.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户成员邀请实体（多渠道：SMS/LINK/QRCODE/EXCEL）。
 *
 * <p>与 V41 sys_org_invite（OrgInvite）解耦：
 * 这里强调"企业开通后的成员引入"，必带 tenant_id + channel + inviter_id，并支持
 * Excel 批量导入失败行回写（fail_reason）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_member_invite")
public class TenantMemberInvite extends BaseEntity {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_REVOKED = 2;
    public static final int STATUS_EXPIRED = 3;

    /** 渠道：SMS / LINK / QRCODE / EXCEL */
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_LINK = "LINK";
    public static final String CHANNEL_QRCODE = "QRCODE";
    public static final String CHANNEL_EXCEL = "EXCEL";

    /** 租户ID */
    private Long tenantId;

    /** 邀请码 */
    private String inviteCode;

    /** 邀请渠道 */
    private String channel;

    /** 被邀请手机号 */
    private String phone;

    /** 被邀请邮箱 */
    private String email;

    /** 默认部门 */
    private Long deptId;

    /** 角色 ID 列表，逗号分隔 */
    private String roleIds;

    /** 邀请人ID */
    private Long inviterId;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 状态 */
    private Integer status;

    /** 接受人用户ID */
    private Long acceptedUserId;

    /** 接受时间 */
    private LocalDateTime acceptedAt;

    /** 失败原因（Excel 行回写） */
    private String failReason;

    /** 角色 ID 列表（非数据库字段） */
    @TableField(exist = false)
    private List<Long> roleIdList;
}
