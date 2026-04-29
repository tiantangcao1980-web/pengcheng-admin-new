package com.pengcheng.hr.okr.dto;

import lombok.Data;

/**
 * Check-in 提交请求 DTO
 */
@Data
public class CheckinDTO {

    private Long objectiveId;

    /** 可选：具体 KR */
    private Long keyResultId;

    private Long userId;

    /** 周次 1-52 */
    private Integer weekIndex;

    /** 进度 0-100 */
    private Integer progress;

    /** 信心指数 1-10 */
    private Integer confidence;

    private String summary;

    /** 阻碍 */
    private String issues;

    private String nextSteps;
}
