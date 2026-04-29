package com.pengcheng.realty.pipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityMoveStageDTO {

    /** 商机ID */
    private Long opportunityId;

    /** 目标阶段ID */
    private Long toStageId;

    /** 操作人ID */
    private Long operatorId;

    /** 备注（流失到 LOST 时建议必填） */
    private String remark;
}
