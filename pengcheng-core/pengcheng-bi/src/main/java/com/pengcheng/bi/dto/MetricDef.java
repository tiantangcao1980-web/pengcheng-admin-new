package com.pengcheng.bi.dto;

import lombok.Data;

/**
 * 视图模型指标定义（存储在 bi_view_model.metrics JSON 数组元素）。
 */
@Data
public class MetricDef {

    /** 指标 key（唯一，用于白名单校验） */
    private String key;

    /** 显示标签 */
    private String label;

    /**
     * 聚合公式：SUM / AVG / COUNT / MAX / MIN / COUNT_DISTINCT。
     * 引擎内部映射为 SQL 聚合函数，不接受用户自定义公式。
     */
    private String formula;

    /** 底层 SQL 列名（不暴露给用户） */
    private String column;
}
