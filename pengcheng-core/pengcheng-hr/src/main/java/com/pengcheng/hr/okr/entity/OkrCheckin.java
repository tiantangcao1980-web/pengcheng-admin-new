package com.pengcheng.hr.okr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OKR 周期回顾（Check-in）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("okr_checkin")
public class OkrCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long objectiveId;

    /** 可选：具体针对某 KR 的 check-in */
    private Long keyResultId;

    private Long userId;

    /** 周次 1-52 */
    private Integer weekIndex;

    /** 本次进度 0-100 */
    private Integer progress;

    /** 信心指数 1-10 */
    private Integer confidence;

    private String summary;

    /** 阻碍 */
    private String issues;

    private String nextSteps;

    private LocalDateTime createTime;
}
