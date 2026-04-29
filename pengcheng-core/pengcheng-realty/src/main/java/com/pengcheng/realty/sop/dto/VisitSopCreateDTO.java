package com.pengcheng.realty.sop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建带看 SOP 请求 DTO
 */
@Data
public class VisitSopCreateDTO {

    /** 客户 ID（必填） */
    private Long customerId;

    /** 楼盘 ID（必填） */
    private Long projectId;

    /** 陪同销售人员 ID（必填） */
    private Long salespersonId;

    /** 渠道联盟商 ID（可选） */
    private Long allianceId;

    /** 带看时间（必填） */
    private LocalDateTime visitTime;

    /** 主推房源 ID（可选） */
    private Long visitUnitId;

    /** 带看时长（分钟，可选） */
    private Integer durationMin;

    /**
     * 有效期天数（默认 14 天）。
     * 超过此期限未签署则状态变为 EXPIRED。
     */
    private Integer validDays;

    /** 客户手机号（用于发送 e签宝签署通知） */
    private String customerPhone;

    /** 客户姓名（用于填入确认书） */
    private String customerName;

    /** 楼盘名称（用于填入确认书，可从数据库查，也可由前端传入） */
    private String projectName;

    /** 陪同销售姓名（用于填入确认书） */
    private String salespersonName;

    /** 渠道名称（用于填入确认书） */
    private String allianceName;

    /** e签宝回调 URL（可选，不传使用全局配置） */
    private String callbackUrl;
}
