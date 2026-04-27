package com.pengcheng.system.smarttable.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 条件 DSL 评估器
 *
 * <p>DSL 格式（JSON）：
 * <pre>
 * // 逻辑组合
 * {"op": "AND", "children": [...]}
 * {"op": "OR",  "children": [...]}
 * {"op": "NOT", "children": [singleChild]}
 *
 * // 叶子条件
 * {"op": "EQ",   "field": "status",  "value": "done"}
 * {"op": "NEQ",  "field": "status",  "value": "pending"}
 * {"op": "GT",   "field": "amount",  "value": 100}
 * {"op": "GTE",  "field": "amount",  "value": 100}
 * {"op": "LT",   "field": "amount",  "value": 100}
 * {"op": "LTE",  "field": "amount",  "value": 100}
 * {"op": "IN",   "field": "type",    "value": ["a","b"]}
 * {"op": "LIKE", "field": "title",   "value": "前缀%"}
 * {"op": "EMPTY","field": "remark"}
 * {"op": "NOT_EMPTY","field": "remark"}
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionEvaluator {

    private final ObjectMapper objectMapper;

    /**
     * 评估条件
     *
     * @param conditionJson 条件 JSON，null 或空字符串视为无条件（返回 true）
     * @param row           行数据 key=fieldKey, value=字段值
     * @return true 表示条件满足
     */
    public boolean evaluate(String conditionJson, Map<String, Object> row) {
        if (conditionJson == null || conditionJson.isBlank()) {
            return true;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> node = objectMapper.readValue(conditionJson, Map.class);
            return evalNode(node, row);
        } catch (Exception e) {
            log.warn("条件 DSL 解析失败，跳过（视为满足）: {}", e.getMessage());
            return true;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean evalNode(Map<String, Object> node, Map<String, Object> row) {
        String op = String.valueOf(node.get("op")).toUpperCase();
        switch (op) {
            case "AND": {
                List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
                if (children == null || children.isEmpty()) return true;
                for (Map<String, Object> child : children) {
                    if (!evalNode(child, row)) return false;
                }
                return true;
            }
            case "OR": {
                List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
                if (children == null || children.isEmpty()) return false;
                for (Map<String, Object> child : children) {
                    if (evalNode(child, row)) return true;
                }
                return false;
            }
            case "NOT": {
                List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
                if (children == null || children.isEmpty()) return true;
                return !evalNode(children.get(0), row);
            }
            default:
                return evalLeaf(op, node, row);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean evalLeaf(String op, Map<String, Object> node, Map<String, Object> row) {
        String field = (String) node.get("field");
        if (row == null) {
            // NULL 安全：行数据为空时，EMPTY 算满足，其余算不满足
            return "EMPTY".equals(op);
        }
        Object cellVal = row.get(field);

        switch (op) {
            case "EMPTY":
                return cellVal == null || String.valueOf(cellVal).isBlank();
            case "NOT_EMPTY":
                return cellVal != null && !String.valueOf(cellVal).isBlank();
            default:
                break;
        }

        // 字段不存在时，其余运算符均不满足
        if (cellVal == null) {
            return false;
        }

        Object condVal = node.get("value");
        switch (op) {
            case "EQ":
                return compareObjects(cellVal, condVal) == 0;
            case "NEQ":
                return compareObjects(cellVal, condVal) != 0;
            case "GT":
                return compareObjects(cellVal, condVal) > 0;
            case "GTE":
                return compareObjects(cellVal, condVal) >= 0;
            case "LT":
                return compareObjects(cellVal, condVal) < 0;
            case "LTE":
                return compareObjects(cellVal, condVal) <= 0;
            case "IN": {
                if (condVal instanceof Collection) {
                    for (Object item : (Collection<?>) condVal) {
                        if (compareObjects(cellVal, item) == 0) return true;
                    }
                }
                return false;
            }
            case "LIKE": {
                String pattern = String.valueOf(condVal);
                String cellStr = String.valueOf(cellVal);
                if (pattern.endsWith("%") && pattern.startsWith("%")) {
                    return cellStr.contains(pattern.substring(1, pattern.length() - 1));
                } else if (pattern.endsWith("%")) {
                    return cellStr.startsWith(pattern.substring(0, pattern.length() - 1));
                } else if (pattern.startsWith("%")) {
                    return cellStr.endsWith(pattern.substring(1));
                }
                return cellStr.equals(pattern);
            }
            default:
                log.warn("未知条件运算符: {}", op);
                return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareObjects(Object a, Object b) {
        // 尝试数值比较
        try {
            double da = Double.parseDouble(String.valueOf(a));
            double db = Double.parseDouble(String.valueOf(b));
            return Double.compare(da, db);
        } catch (NumberFormatException ignored) {
            // 字符串比较
            return String.valueOf(a).compareTo(String.valueOf(b));
        }
    }
}
