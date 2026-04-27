package com.pengcheng.bi.dto;

import lombok.Data;

import java.util.List;

/**
 * BI 查询过滤条件。
 *
 * <p>{@code column} 必须在视图模型维度/指标白名单中，否则引擎拒绝请求。
 * {@code values} 全部通过 PreparedStatement 参数化占位，不做字符串拼接。
 */
@Data
public class Filter {

    /**
     * 列 key（与 BiViewModel dimensions/metrics 中的 key 对应）。
     */
    private String column;

    /**
     * 操作符。
     */
    private FilterOp op;

    /**
     * 过滤值列表（EQ/NEQ/GT/GTE/LT/LTE 取第 0 个；IN 取全部；BETWEEN 取 [0,1]）。
     */
    private List<Object> values;
}
