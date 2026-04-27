package com.pengcheng.hr.okr.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 KR 当前值请求 DTO
 */
@Data
public class UpdateProgressDTO {

    private Long keyResultId;

    /** 最新实际值 */
    private BigDecimal currentValue;

    /** 手动覆盖进度（可选，优先用 currentValue 自动算） */
    private Integer progress;
}
