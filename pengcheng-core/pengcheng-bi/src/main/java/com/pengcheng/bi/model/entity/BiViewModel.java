package com.pengcheng.bi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BI 视图模型实体（bi_view_model）。
 *
 * <p>dimensions / metrics 以 JSON 文本存储，由 {@link com.pengcheng.bi.service.BiViewModelService}
 * 反序列化为 {@link com.pengcheng.bi.dto.DimensionDef} / {@link com.pengcheng.bi.dto.MetricDef} 列表。
 */
@Data
@TableName("bi_view_model")
public class BiViewModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 视图编码，全局唯一 */
    private String code;

    /** 视图名称 */
    private String name;

    /** 主表名（白名单，只允许管理员配置） */
    private String baseTable;

    /** 可选 JOIN 子句（管理员配置，暂不暴露给普通用户修改） */
    private String joinClause;

    /**
     * 维度定义 JSON（{@code List<DimensionDef>}）。
     * 列名：dimensions
     */
    @TableField("dimensions")
    private String dimensionsJson;

    /**
     * 指标定义 JSON（{@code List<MetricDef>}）。
     * 列名：metrics
     */
    @TableField("metrics")
    private String metricsJson;

    /** 是否启用（1=启用，0=禁用） */
    private Integer enabled;

    private LocalDateTime createTime;
}
