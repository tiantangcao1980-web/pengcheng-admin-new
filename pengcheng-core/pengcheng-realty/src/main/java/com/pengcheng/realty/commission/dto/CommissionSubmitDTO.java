package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 佣金提交审批 DTO（业务员提交）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSubmitDTO {

    /** 佣金ID（必填） */
    private Long commissionId;

    /** 提交人ID */
    private Long submitterId;

    /** 提交备注（可选） */
    private String remark;
}
