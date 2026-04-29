package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 佣金录入 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionCreateDTO {

    /** 关联成交记录ID（必填） */
    private Long dealId;

    /** 项目ID（必填） */
    private Long projectId;

    /** 联盟商ID（必填） */
    private Long allianceId;

    /** 应收佣金（必填） */
    private BigDecimal receivableAmount;

    /** 应结佣金（必填） */
    private BigDecimal payableAmount;

    /** 公司平台费（必填） */
    private BigDecimal platformFee;

    /**
     * 物业类型（V17 新增，决定 ProjectCommissionRule 选哪条）
     * 取值：RESIDENTIAL/COMMERCIAL/APARTMENT/OFFICE/OTHER
     * 不传时按 RESIDENTIAL 处理
     */
    private String propertyType;

    /**
     * 客户籍贯（V17 新增）：DOMESTIC内地 / OVERSEAS境外
     * 不传时按 DOMESTIC 处理
     */
    private String customerOrigin;

    /** 佣金明细 */
    private CommissionDetailDTO detail;
}
