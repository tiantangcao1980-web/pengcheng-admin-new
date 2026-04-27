package com.pengcheng.bi.dto;

import lombok.Data;

/**
 * BI 查询排序参数。
 *
 * <p>{@code column} 必须在视图模型白名单 key 中，引擎直接用对应的 SQL column 表达式，
 * 不接受用户任意字符串拼入 ORDER BY 子句。
 */
@Data
public class Sort {

    /**
     * 排序列 key（与 BiViewModel dimensions/metrics 中的 key 对应）。
     */
    private String column;

    /**
     * 升序/降序，默认 ASC。
     */
    private Direction direction = Direction.ASC;

    public enum Direction {
        ASC, DESC
    }
}
