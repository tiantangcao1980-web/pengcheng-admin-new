package com.pengcheng.bi.engine.impl;

import com.pengcheng.bi.dto.Column;
import com.pengcheng.bi.dto.DimensionDef;
import com.pengcheng.bi.dto.Filter;
import com.pengcheng.bi.dto.MetricDef;
import com.pengcheng.bi.dto.Sort;
import com.pengcheng.bi.engine.BiQueryEngine;
import com.pengcheng.bi.engine.BiQueryRequest;
import com.pengcheng.bi.engine.BiQueryResponse;
import com.pengcheng.bi.model.entity.BiViewModel;
import com.pengcheng.bi.service.BiViewModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 JdbcTemplate 的 BI 多维查询引擎实现。
 *
 * <h2>SQL 注入防护要点</h2>
 * <ol>
 *   <li><b>列名白名单</b>：所有 SELECT / GROUP BY / ORDER BY 表达式均来自
 *       {@link BiViewModel} 中管理员预设的 {@code column} 字段，绝不拼接用户提交的原始字符串。</li>
 *   <li><b>参数化值</b>：WHERE 子句的所有 value 通过 {@code PreparedStatement ?} 占位，
 *       由 {@code JdbcTemplate#query(String, Object[], ...)} 处理。</li>
 *   <li><b>字段 key 校验</b>：用户传入的 dimensions/metrics key 和 filter.column 均先与
 *       白名单 Set 做交集校验，不在白名单的 key 立即抛 {@link IllegalArgumentException}。</li>
 *   <li><b>聚合函数白名单</b>：指标 formula 只能是预设的 5 种，不允许自定义 SQL 片段。</li>
 *   <li><b>limit 强制截断</b>：用户 limit 超过 10000 时强制截断为 10000。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class JdbcBiQueryEngine implements BiQueryEngine {

    /** 允许的聚合函数白名单（COUNT_DISTINCT 单独处理） */
    private static final Set<String> ALLOWED_FORMULAS =
            Set.of("SUM", "AVG", "COUNT", "MAX", "MIN", "COUNT_DISTINCT");

    /** limit 最大上限 */
    static final int MAX_LIMIT = 10000;

    private final BiViewModelService viewModelService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public BiQueryResponse execute(BiQueryRequest req) {
        // ---- 1. 加载视图模型 ----
        BiViewModel vm = viewModelService.findByCode(req.getViewCode());
        if (vm == null) {
            throw new IllegalArgumentException("视图不存在或未启用：" + req.getViewCode());
        }

        List<DimensionDef> allDims  = viewModelService.parseDimensions(vm);
        List<MetricDef>    allMets  = viewModelService.parseMetrics(vm);

        // 维度/指标 key → def 映射（用于白名单查找）
        Map<String, DimensionDef> dimMap = allDims.stream()
                .collect(Collectors.toMap(DimensionDef::getKey, d -> d));
        Map<String, MetricDef> metMap = allMets.stream()
                .collect(Collectors.toMap(MetricDef::getKey, m -> m));

        // 所有合法 key（维度 + 指标），用于 filter.column 白名单校验
        Set<String> allKeys = new java.util.HashSet<>(dimMap.keySet());
        allKeys.addAll(metMap.keySet());

        // ---- 2. 白名单校验 ----
        validateKeys("dimension", req.getDimensions(), dimMap.keySet());
        validateKeys("metric",    req.getMetrics(),    metMap.keySet());

        if (!CollectionUtils.isEmpty(req.getFilters())) {
            for (Filter f : req.getFilters()) {
                if (!allKeys.contains(f.getColumn())) {
                    throw new IllegalArgumentException(
                            "filter.column 不在白名单中，拒绝请求：" + f.getColumn());
                }
            }
        }

        if (req.getSort() != null && req.getSort().getColumn() != null) {
            if (!allKeys.contains(req.getSort().getColumn())) {
                throw new IllegalArgumentException(
                        "sort.column 不在白名单中，拒绝请求：" + req.getSort().getColumn());
            }
        }

        // ---- 3. 限制 limit ----
        int limit = req.getLimit() <= 0 ? 100 : Math.min(req.getLimit(), MAX_LIMIT);

        // ---- 4. 构建 SELECT 子句 ----
        // 列定义（顺序：维度在前，指标在后）
        List<Column>  resultColumns = new ArrayList<>();
        List<String>  selectExprs   = new ArrayList<>();
        List<String>  groupByExprs  = new ArrayList<>();

        // 维度列
        List<String> reqDims = CollectionUtils.isEmpty(req.getDimensions())
                ? List.of() : req.getDimensions();
        for (String key : reqDims) {
            DimensionDef def = dimMap.get(key);
            String expr = buildDimExpr(def);
            selectExprs.add(expr + " AS `" + key + "`");
            groupByExprs.add(expr);
            resultColumns.add(new Column(key, def.getLabel(), mapDimType(def.getType())));
        }

        // 指标列
        List<String> reqMets = CollectionUtils.isEmpty(req.getMetrics())
                ? List.of() : req.getMetrics();
        for (String key : reqMets) {
            MetricDef def = metMap.get(key);
            String aggExpr = buildAggExpr(def);
            selectExprs.add(aggExpr + " AS `" + key + "`");
            resultColumns.add(new Column(key, def.getLabel(), "number"));
        }

        // ---- 5. 构建 WHERE 子句 + 参数列表 ----
        List<Object> params     = new ArrayList<>();
        List<String> whereClauses = new ArrayList<>();

        if (!CollectionUtils.isEmpty(req.getFilters())) {
            for (Filter f : req.getFilters()) {
                // 从白名单中取 SQL 列表达式（不使用用户提供的原始 column 字符串）
                String sqlExpr = resolveSqlExpr(f.getColumn(), dimMap, metMap);
                buildWhereClause(f, sqlExpr, whereClauses, params);
            }
        }

        // ---- 6. 构建 ORDER BY 子句 ----
        String orderByClause = "";
        if (req.getSort() != null && req.getSort().getColumn() != null) {
            String sortSqlExpr = resolveSqlExpr(req.getSort().getColumn(), dimMap, metMap);
            // direction 来自枚举，不接受用户自定义字符串
            String dir = (req.getSort().getDirection() == Sort.Direction.DESC) ? "DESC" : "ASC";
            orderByClause = " ORDER BY " + sortSqlExpr + " " + dir;
        }

        // ---- 7. 拼装最终 SQL ----
        // 仅拼接来自白名单的 SQL 片段；用户输入值全部通过 ? 占位
        String baseFrom = vm.getBaseTable()
                + (vm.getJoinClause() != null ? " " + vm.getJoinClause() : "");

        String selectClause  = String.join(", ", selectExprs);
        String whereClause   = whereClauses.isEmpty() ? ""
                : " WHERE " + String.join(" AND ", whereClauses);
        String groupByClause = groupByExprs.isEmpty() ? ""
                : " GROUP BY " + String.join(", ", groupByExprs);

        String dataSql = "SELECT " + selectClause
                + " FROM " + baseFrom
                + whereClause
                + groupByClause
                + orderByClause
                + " LIMIT " + limit;

        String countSql = "SELECT COUNT(*) FROM (" +
                "SELECT 1 FROM " + baseFrom + whereClause + groupByClause
                + ") _bi_count";

        Object[] paramsArray = params.toArray();

        // ---- 8. 执行查询 ----
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql, paramsArray);
        long totalRows = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                countSql, paramsArray, Long.class)) ? 0L
                : jdbcTemplate.queryForObject(countSql, paramsArray, Long.class);

        // 将 LinkedHashMap 转换，保持顺序
        List<Map<String, Object>> resultRows = rows.stream()
                .map(row -> {
                    Map<String, Object> ordered = new LinkedHashMap<>();
                    resultColumns.forEach(c -> ordered.put(c.getKey(), row.get(c.getKey())));
                    return ordered;
                })
                .collect(Collectors.toList());

        return new BiQueryResponse(resultColumns, resultRows, totalRows);
    }

    // ====================================================================
    // 内部工具方法
    // ====================================================================

    /**
     * 白名单 key 校验。
     *
     * @param kind      "dimension" 或 "metric"（用于错误信息）
     * @param requested 用户请求的 key 列表
     * @param whitelist 允许的 key 集合
     * @throws IllegalArgumentException 若存在不在白名单中的 key
     */
    void validateKeys(String kind, List<String> requested, Set<String> whitelist) {
        if (CollectionUtils.isEmpty(requested)) {
            return;
        }
        for (String key : requested) {
            if (!whitelist.contains(key)) {
                throw new IllegalArgumentException(
                        kind + " key 不在白名单中，拒绝请求：" + key);
            }
        }
    }

    /**
     * 根据维度类型生成 SQL 表达式（日期维度特殊处理）。
     * <p>返回的表达式来自白名单配置，不含任何用户输入。
     */
    private String buildDimExpr(DimensionDef def) {
        return switch (def.getType()) {
            case "date_month" -> "DATE_FORMAT(`" + def.getColumn() + "`, '%Y-%m')";
            case "date_day"   -> "DATE(`" + def.getColumn() + "`)";
            case "date_year"  -> "YEAR(`" + def.getColumn() + "`)";
            default           -> "`" + def.getColumn() + "`";
        };
    }

    /**
     * 生成聚合指标 SQL 表达式（formula 来自白名单）。
     */
    private String buildAggExpr(MetricDef def) {
        String formula = def.getFormula().toUpperCase();
        if (!ALLOWED_FORMULAS.contains(formula)) {
            throw new IllegalArgumentException("不支持的聚合函数：" + def.getFormula());
        }
        String col = "`" + def.getColumn() + "`";
        if ("COUNT_DISTINCT".equals(formula)) {
            return "COUNT(DISTINCT " + col + ")";
        }
        return formula + "(" + col + ")";
    }

    /**
     * 通过白名单 key 解析对应的 SQL 表达式（用于 WHERE / ORDER BY）。
     */
    private String resolveSqlExpr(String key,
                                   Map<String, DimensionDef> dimMap,
                                   Map<String, MetricDef> metMap) {
        if (dimMap.containsKey(key)) {
            return buildDimExpr(dimMap.get(key));
        }
        if (metMap.containsKey(key)) {
            return buildAggExpr(metMap.get(key));
        }
        // 此处理论上不会到达（调用前已做白名单校验）
        throw new IllegalArgumentException("key 不在白名单中：" + key);
    }

    /**
     * 构建单个 WHERE 子句，value 全部通过 PreparedStatement ? 占位。
     *
     * @param f           过滤条件
     * @param sqlExpr     来自白名单的 SQL 列表达式
     * @param clauses     WHERE 子句列表（追加）
     * @param params      参数列表（追加）
     */
    private void buildWhereClause(Filter f, String sqlExpr,
                                   List<String> clauses, List<Object> params) {
        List<Object> vals = f.getValues();
        switch (f.getOp()) {
            case EQ    -> { clauses.add(sqlExpr + " = ?");  params.add(val(vals, 0)); }
            case NEQ   -> { clauses.add(sqlExpr + " != ?"); params.add(val(vals, 0)); }
            case GT    -> { clauses.add(sqlExpr + " > ?");  params.add(val(vals, 0)); }
            case GTE   -> { clauses.add(sqlExpr + " >= ?"); params.add(val(vals, 0)); }
            case LT    -> { clauses.add(sqlExpr + " < ?");  params.add(val(vals, 0)); }
            case LTE   -> { clauses.add(sqlExpr + " <= ?"); params.add(val(vals, 0)); }
            case LIKE  -> { clauses.add(sqlExpr + " LIKE ?"); params.add(val(vals, 0)); }
            case IN    -> {
                if (CollectionUtils.isEmpty(vals)) {
                    throw new IllegalArgumentException("IN 过滤条件 values 不能为空");
                }
                String placeholders = vals.stream().map(v -> "?")
                        .collect(Collectors.joining(", "));
                clauses.add(sqlExpr + " IN (" + placeholders + ")");
                params.addAll(vals);
            }
            case BETWEEN -> {
                if (vals == null || vals.size() < 2) {
                    throw new IllegalArgumentException("BETWEEN 需要 2 个 values");
                }
                clauses.add(sqlExpr + " BETWEEN ? AND ?");
                params.add(vals.get(0));
                params.add(vals.get(1));
            }
        }
    }

    private Object val(List<Object> vals, int index) {
        if (CollectionUtils.isEmpty(vals) || vals.size() <= index) {
            throw new IllegalArgumentException("过滤条件 values 数量不足");
        }
        return vals.get(index);
    }

    private String mapDimType(String type) {
        if (type == null) return "string";
        return switch (type) {
            case "number"     -> "number";
            case "date_day", "date_month", "date_year" -> "date";
            default           -> "string";
        };
    }
}
