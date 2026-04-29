package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.formula.FormulaAst.*;
import com.pengcheng.system.smarttable.entity.SmartTableField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 公式求值器
 *
 * 内置函数：SUM, AVG, MAX, MIN, IF, CONCAT, UPPER, LOWER, LEN,
 *           ROUND, NOW, TODAY, DATEDIFF
 *
 * 类型系统（宽容 Excel 风格）：
 *   - 数字运算：自动 coerce 字符串→Double
 *   - 字符串连接（+）：任意类型 toString
 *   - 布尔：0/""/null/false → false
 *   - 异常 → "#ERROR!" 而非抛出
 *
 * 循环引用保护：最大递归深度 10（由调用方通过 depth 参数传入）
 */
public class FormulaEvaluator {

    private static final int MAX_DEPTH = 10;
    private static final String ERROR = "#ERROR!";

    private final Map<String, Object> row;
    private final Map<String, SmartTableField> fieldMap;

    public FormulaEvaluator(Map<String, Object> row, List<SmartTableField> fields) {
        this.row = row == null ? Collections.emptyMap() : row;
        this.fieldMap = new HashMap<>();
        if (fields != null) {
            for (SmartTableField f : fields) {
                fieldMap.put(f.getFieldKey(), f);
            }
        }
    }

    /**
     * 对外求值入口，异常统一转为 "#ERROR!"
     */
    public Object evaluate(Node ast) {
        try {
            return eval(ast, 0);
        } catch (Exception e) {
            return ERROR;
        }
    }

    // ========================= 核心求值 =========================

    private Object eval(Node node, int depth) {
        if (depth > MAX_DEPTH) throw new RuntimeException("公式递归深度超限，疑似循环引用");

        if (node instanceof NumberLiteral)  return ((NumberLiteral) node).value;
        if (node instanceof StringLiteral)  return ((StringLiteral) node).value;
        if (node instanceof BooleanLiteral) return ((BooleanLiteral) node).value;
        if (node instanceof NullLiteral)    return null;

        if (node instanceof FieldRefNode) {
            String key = ((FieldRefNode) node).fieldKey;
            return row.get(key);   // 字段不存在时返回 null（null-safe）
        }

        if (node instanceof UnaryOpNode)     return evalUnary((UnaryOpNode) node, depth);
        if (node instanceof BinaryOpNode)    return evalBinary((BinaryOpNode) node, depth);
        if (node instanceof TernaryNode)     return evalTernary((TernaryNode) node, depth);
        if (node instanceof FunctionCallNode) return evalFunction((FunctionCallNode) node, depth);

        throw new RuntimeException("未知 AST 节点: " + node.getClass().getSimpleName());
    }

    // ========================= 一元运算 =========================

    private Object evalUnary(UnaryOpNode n, int depth) {
        Object val = eval(n.operand, depth + 1);
        switch (n.op) {
            case "!": return !isTruthy(val);
            case "-": return -toDouble(val);
            default:  throw new RuntimeException("未知一元运算符: " + n.op);
        }
    }

    // ========================= 二元运算 =========================

    private Object evalBinary(BinaryOpNode n, int depth) {
        Object left  = eval(n.left,  depth + 1);
        Object right = eval(n.right, depth + 1);

        switch (n.op) {
            // 算术（+ 支持字符串拼接）
            case "+":
                if (left instanceof String || right instanceof String) {
                    return toStr(left) + toStr(right);
                }
                return toDouble(left) + toDouble(right);
            case "-":  return toDouble(left) - toDouble(right);
            case "*":  return toDouble(left) * toDouble(right);
            case "/":  {
                double d = toDouble(right);
                if (d == 0) throw new RuntimeException("除以零");
                return toDouble(left) / d;
            }
            case "%":  return toDouble(left) % toDouble(right);

            // 比较
            case "==": return objectEquals(left, right);
            case "!=": return !objectEquals(left, right);
            case ">":  return toDouble(left) >  toDouble(right);
            case ">=": return toDouble(left) >= toDouble(right);
            case "<":  return toDouble(left) <  toDouble(right);
            case "<=": return toDouble(left) <= toDouble(right);

            // 逻辑
            case "&&": return isTruthy(left) && isTruthy(right);
            case "||": return isTruthy(left) || isTruthy(right);

            default: throw new RuntimeException("未知二元运算符: " + n.op);
        }
    }

    // ========================= 三元运算 =========================

    private Object evalTernary(TernaryNode n, int depth) {
        Object cond = eval(n.condition, depth + 1);
        return isTruthy(cond) ? eval(n.thenExpr, depth + 1) : eval(n.elseExpr, depth + 1);
    }

    // ========================= 函数调用 =========================

    private Object evalFunction(FunctionCallNode n, int depth) {
        List<Object> args = new ArrayList<>();
        for (Node arg : n.args) {
            args.add(eval(arg, depth + 1));
        }

        switch (n.name) {
            case "SUM":    return sum(args);
            case "AVG":    return avg(args);
            case "MAX":    return max(args);
            case "MIN":    return min(args);
            case "IF":     return funcIf(args);
            case "CONCAT": return concat(args);
            case "UPPER":  return toStr(requireArg(args, 0, "UPPER")).toUpperCase();
            case "LOWER":  return toStr(requireArg(args, 0, "LOWER")).toLowerCase();
            case "LEN":    return (double) toStr(requireArg(args, 0, "LEN")).length();
            case "ROUND":  return funcRound(args);
            case "NOW":    return LocalDateTime.now().toString();
            case "TODAY":  return LocalDate.now().toString();
            case "DATEDIFF": return funcDateDiff(args);
            default: throw new RuntimeException("未知函数: " + n.name);
        }
    }

    // ----- 内置函数实现 -----

    private double sum(List<Object> args) {
        double s = 0;
        for (Object a : args) { if (a != null) s += toDouble(a); }
        return s;
    }

    private double avg(List<Object> args) {
        if (args.isEmpty()) throw new RuntimeException("AVG 需至少一个参数");
        long count = args.stream().filter(Objects::nonNull).count();
        if (count == 0) return 0;
        return sum(args) / count;
    }

    private double max(List<Object> args) {
        return args.stream().filter(Objects::nonNull)
                .mapToDouble(this::toDouble).max()
                .orElseThrow(() -> new RuntimeException("MAX 无有效参数"));
    }

    private double min(List<Object> args) {
        return args.stream().filter(Objects::nonNull)
                .mapToDouble(this::toDouble).min()
                .orElseThrow(() -> new RuntimeException("MIN 无有效参数"));
    }

    private Object funcIf(List<Object> args) {
        if (args.size() < 2) throw new RuntimeException("IF 需要 2-3 个参数");
        boolean cond = isTruthy(args.get(0));
        if (cond) return args.get(1);
        return args.size() > 2 ? args.get(2) : null;
    }

    private String concat(List<Object> args) {
        StringBuilder sb = new StringBuilder();
        for (Object a : args) sb.append(toStr(a));
        return sb.toString();
    }

    private double funcRound(List<Object> args) {
        if (args.isEmpty()) throw new RuntimeException("ROUND 需至少一个参数");
        double num    = toDouble(args.get(0));
        int    digits = args.size() > 1 ? (int) toDouble(args.get(1)) : 0;
        double factor = Math.pow(10, digits);
        return Math.round(num * factor) / factor;
    }

    private double funcDateDiff(List<Object> args) {
        if (args.size() < 2) throw new RuntimeException("DATEDIFF 需要 2-3 个参数");
        String unit = args.size() > 2 ? toStr(args.get(2)).toLowerCase() : "day";
        LocalDate d1 = parseDate(args.get(0));
        LocalDate d2 = parseDate(args.get(1));
        switch (unit) {
            case "day":   return (double) ChronoUnit.DAYS.between(d1, d2);
            case "month": return (double) ChronoUnit.MONTHS.between(d1, d2);
            case "year":  return (double) ChronoUnit.YEARS.between(d1, d2);
            default: throw new RuntimeException("DATEDIFF 不支持的单位: " + unit);
        }
    }

    // ========================= 类型工具 =========================

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof Boolean) return ((Boolean) v) ? 1.0 : 0.0;
        try { return Double.parseDouble(v.toString().trim()); }
        catch (NumberFormatException e) { throw new RuntimeException("无法将 \"" + v + "\" 转换为数字"); }
    }

    private String toStr(Object v) {
        if (v == null) return "";
        if (v instanceof Double) {
            double d = (Double) v;
            // 避免 1.0 显示为 "1.0"
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf((long) d);
            }
        }
        return v.toString();
    }

    private boolean isTruthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).doubleValue() != 0;
        if (v instanceof String) return !((String) v).isEmpty();
        return true;
    }

    private boolean objectEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // 数字比较
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    private LocalDate parseDate(Object v) {
        if (v == null) throw new RuntimeException("日期参数不能为 null");
        String s = toStr(v).trim();
        if (s.length() >= 10) s = s.substring(0, 10);
        return LocalDate.parse(s);
    }

    private Object requireArg(List<Object> args, int idx, String funcName) {
        if (idx >= args.size()) throw new RuntimeException(funcName + " 缺少第 " + (idx + 1) + " 个参数");
        return args.get(idx);
    }
}
