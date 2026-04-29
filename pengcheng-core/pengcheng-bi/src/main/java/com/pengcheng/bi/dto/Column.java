package com.pengcheng.bi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BI 查询结果列元数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Column {

    /** 列 key（前端用于映射行数据） */
    private String key;

    /** 列显示名称 */
    private String label;

    /** 列类型（string / number / date） */
    private String type;
}
