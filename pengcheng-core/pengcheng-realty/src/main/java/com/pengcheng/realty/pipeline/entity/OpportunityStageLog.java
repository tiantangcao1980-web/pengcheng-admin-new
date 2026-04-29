package com.pengcheng.realty.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商机阶段流转日志（审计 + 时间线展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_opportunity_stage_log")
public class OpportunityStageLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long opportunityId;

    /** 原阶段ID（创建时为空） */
    private Long fromStageId;

    /** 新阶段ID */
    private Long toStageId;

    private Long operatorId;

    private String remark;

    private LocalDateTime changeTime;
}
