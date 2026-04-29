package com.pengcheng.realty.customer.field.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售外勤拜访
 *
 * 与 CustomerVisit（客户到访案场）解耦：
 *   - CustomerVisit ：客户来项目案场
 *   - FieldVisit    ：销售去客户/楼盘
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_field_visit")
public class FieldVisit extends BaseEntity {

    private Long userId;

    private Long customerId;

    private Long projectId;

    /** 1客户拜访 2楼盘踏勘 3带看 4其他 */
    private Integer visitType;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String address;

    /** 拍照 OSS URL 数组（逗号分隔） */
    private String photoUrls;

    private String purpose;

    private String result;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private Integer durationMinutes;

    private Long tenantId;

    private String extra;

    public static final int TYPE_CUSTOMER_VISIT = 1;
    public static final int TYPE_PROJECT_INSPECT = 2;
    public static final int TYPE_PROPERTY_TOUR = 3;
    public static final int TYPE_OTHER = 4;
}
