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
 * 销售漏斗阶段定义
 *
 * 默认 6 个阶段（V11 SQL 初始化）：留资 / 意向 / 看房 / 认筹 / 签约(终态) / 流失(终态)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_pipeline_stage")
public class PipelineStage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 阶段名称 */
    private String name;

    /** 阶段代码：LEAD / INTENT / VISIT / SUBSCRIBE / SIGNED / LOST */
    private String code;

    /** 排序号 */
    private Integer orderNo;

    /** 默认胜率 0-100 */
    private Integer winRate;

    /** 看板列色值 #RRGGBB */
    private String color;

    /** 是否终态：0-否 1-是 */
    private Integer isTerminal;

    /** 是否启用：0-禁用 1-启用 */
    private Integer active;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 阶段代码常量 */
    public static final String CODE_LEAD = "LEAD";
    public static final String CODE_INTENT = "INTENT";
    public static final String CODE_VISIT = "VISIT";
    public static final String CODE_SUBSCRIBE = "SUBSCRIBE";
    public static final String CODE_SIGNED = "SIGNED";
    public static final String CODE_LOST = "LOST";
}
