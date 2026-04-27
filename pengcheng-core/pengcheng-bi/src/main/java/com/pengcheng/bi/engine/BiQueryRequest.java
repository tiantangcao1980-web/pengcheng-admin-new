package com.pengcheng.bi.engine;

import com.pengcheng.bi.dto.Filter;
import com.pengcheng.bi.dto.Sort;
import lombok.Data;

import java.util.List;

/**
 * BI 多维查询请求。
 *
 * <p>所有字段均为用户输入，引擎在执行前对 dimensions / metrics / filter.column
 * 进行严格白名单校验，任何不在白名单内的 key 立即拒绝。
 */
@Data
public class BiQueryRequest {

    /** 视图编码（对应 bi_view_model.code） */
    private String viewCode;

    /**
     * 选中维度 key 列表（必须是 view 维度白名单中的 key）。
     */
    private List<String> dimensions;

    /**
     * 选中指标 key 列表（必须是 view 指标白名单中的 key）。
     */
    private List<String> metrics;

    /**
     * 过滤条件列表（filter.column 必须在维度/指标白名单中）。
     */
    private List<Filter> filters;

    /**
     * 排序参数（sort.column 必须在维度/指标白名单中）。
     */
    private Sort sort;

    /**
     * 返回行数上限，默认 100，最大 10000（引擎强制截断）。
     */
    private int limit = 100;
}
