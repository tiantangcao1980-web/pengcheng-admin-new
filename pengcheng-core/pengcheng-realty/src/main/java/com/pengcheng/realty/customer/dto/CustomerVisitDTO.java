package com.pengcheng.realty.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 客户到访数据录入 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVisitDTO {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 实际到访时间
     */
    private LocalDateTime actualVisitTime;

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

    /**
     * 带看日期（V17 新增，必填）
     */
    private LocalDate visitDate;

    /**
     * 带看时间（V17 新增，选填）
     */
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
     * 关联联盟商或开发商 ID（V17 新增）
     */
    private Long partnerId;
}
