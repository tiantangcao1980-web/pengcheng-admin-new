package com.pengcheng.realty.pipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityCreateDTO {

    /** 客户ID（必填） */
    private Long customerId;

    /** 楼盘项目ID（必填） */
    private Long projectId;

    /** 起始阶段ID，不传则取 LEAD */
    private Long stageId;

    private String title;

    private BigDecimal expectedAmount;

    private LocalDate expectedCloseDate;

    private Long ownerId;

    private String nextAction;
}
