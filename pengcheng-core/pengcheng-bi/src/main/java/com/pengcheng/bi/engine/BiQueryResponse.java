package com.pengcheng.bi.engine;

import com.pengcheng.bi.dto.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * BI 多维查询响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiQueryResponse {

    /** 列元数据列表（顺序与 rows 中的 key 对应） */
    private List<Column> columns;

    /** 数据行列表，每行是 key → value 的 Map */
    private List<Map<String, Object>> rows;

    /** 满足条件的总行数（分页场景使用） */
    private long totalRows;
}
