package com.pengcheng.bi.dto;

import lombok.Data;

/**
 * 视图模型维度定义（存储在 bi_view_model.dimensions JSON 数组元素）。
 */
@Data
public class DimensionDef {

    /** 维度 key（唯一，用于白名单校验） */
    private String key;

    /** 显示标签 */
    private String label;

    /** 底层 SQL 列名或表达式（不暴露给用户，引擎内部使用） */
    private String column;

    /**
     * 类型：string / number / date_day / date_month / date_year。
     * date_month → DATE_FORMAT(col, '%Y-%m')
     * date_day   → DATE(col)
     */
    private String type;
}
