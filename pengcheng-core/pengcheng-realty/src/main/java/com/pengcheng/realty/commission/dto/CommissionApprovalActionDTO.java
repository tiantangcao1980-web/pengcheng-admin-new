package com.pengcheng.realty.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 佣金审批动作 DTO（主管/财务/放款节点共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionApprovalActionDTO {

    /** 佣金ID（必填） */
    private Long commissionId;

    /** 审批人ID（必填） */
    private Long approverId;

    /** 是否通过：true=通过 false=驳回（必填） */
    private Boolean approved;

    /** 审批备注（驳回时必填） */
    private String remark;
}
