package com.pengcheng.oa.correction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请单（attendance_correction）。
 * <p>
 * 与 {@code approval_instance} 通过 bizType="correction" + bizId=this.id 关联，
 * 由审批流引擎驱动审批流转。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_correction")
public class AttendanceCorrection extends BaseEntity {

    /** 补卡类型 1=上班 2=下班 */
    public static final int CORRECTION_TYPE_CLOCK_IN = 1;
    public static final int CORRECTION_TYPE_CLOCK_OUT = 2;

    /** 补卡单状态 */
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_REJECTED = 3;

    /** 申请人 */
    private Long userId;

    /** 补卡日期 */
    private LocalDate correctionDate;

    /** 补卡类型 1=上班 2=下班 */
    private Integer correctionType;

    /** 应该的打卡时间 */
    private LocalDateTime expectedTime;

    /** 补卡原因 */
    private String reason;

    /** 关联流程实例 ID（approval_instance.id） */
    private Long approvalInstanceId;

    /** 状态 1=待审批 2=已通过 3=已驳回 */
    private Integer status;
}
