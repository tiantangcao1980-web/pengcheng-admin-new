package com.pengcheng.realty.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 客户到访记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("customer_visit")
public class CustomerVisit extends BaseEntity {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 实际到访时间（旧字段，保留兼容）
     */
    private LocalDateTime actualVisitTime;

    /**
     * 带看日期（必填，V17 新增）
     */
    private LocalDate visitDate;

    /**
     * 带看时间（选填，独立时间字段，V17 新增）
     */
    @com.baomidou.mybatisplus.annotation.TableField("visit_time_only")
    private LocalTime visitTimeOnly;

    /**
     * 带看公司（V17 新增）
     */
    private String visitCompany;

    /**
     * 用户类型：1-联盟商 2-开发商（V17 新增）
     */
    private Integer userType;

    /**
     * 关联联盟商或开发商 ID（按 userType 路由查询，V17 新增）
     */
    private Long partnerId;

    /**
     * 实际到访人数
     */
    private Integer actualVisitCount;

    /**
     * 接待人员
     */
    private String receptionist;

    /**
     * 备注
     */
    private String remark;

    public static final int USER_TYPE_ALLIANCE = 1;
    public static final int USER_TYPE_DEVELOPER = 2;
}
