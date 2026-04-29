package com.pengcheng.oa.correction.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CorrectionApplyDTO {

    private Long userId;

    private LocalDate correctionDate;

    /** 1=上班 2=下班 */
    private Integer correctionType;

    private LocalDateTime expectedTime;

    private String reason;

    /** 审批流程定义 ID（不传则使用默认补卡流程） */
    private Long flowDefId;
}
