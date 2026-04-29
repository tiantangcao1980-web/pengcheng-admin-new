package com.pengcheng.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 线索实体（V4.0 闭环③）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "crm_lead", autoResultMap = true)
public class CrmLead extends BaseEntity {

    /** 业务编号（lead_no） */
    private String leadNo;

    /** 线索名称（联系人姓名） */
    private String name;

    /** 联系方式（建议加密；MVP 阶段先明文，后续接入 PhoneEncryptTypeHandler） */
    private String phone;

    private String phoneMasked;
    private String email;
    private String wechat;
    private String company;

    /** 来源：form/qrcode/import/manual/api */
    private String source;
    private String sourceDetail;

    /** 意向等级：1-高 2-中 3-低 */
    private Integer intentionLevel;

    /** 状态：1-待分配 2-已分配 3-跟进中 4-已转客户 5-已废弃 */
    private Integer status;

    private Long ownerId;
    private Long deptId;

    private LocalDateTime assignTime;
    private LocalDateTime lastFollowTime;
    private LocalDateTime convertTime;

    /** 转化后的客户 ID */
    private Long customerId;

    private String remark;
    private Long tenantId;
}
