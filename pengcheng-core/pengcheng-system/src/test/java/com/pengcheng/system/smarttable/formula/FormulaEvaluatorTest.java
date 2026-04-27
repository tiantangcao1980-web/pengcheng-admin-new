package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.entity.SmartTableField;
import com.pengcheng.system.smarttable.formula.FormulaAst.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * FormulaEvaluator 单元测试 — 8 用例
 *
 * 覆盖：算术、比较、字符串拼接、IF、SUM 多参、null 安全、
 *       缺失字段返回 null、循环引用深度检测
 */
@DisplayName("FormulaEvaluator — 求值器测试")
class FormulaEvaluatorTest {

    /** 便捷求值：表达式 + 行数据 */
    private Object eval(String expr, Map<String, Object> row) {
        Node ast = new FormulaParser(expr).parse();
        return new FormulaEvaluator(row, Collections.emptyList()).evaluate(ast);
    }

    /** 无行数据 */
    private Object eval(String expr) {
        return eval(expr, Collections.emptyMap());
    }

    // ========================= 1. 算术运算 =========================

    @Test
    @DisplayName("TC-E01: 基础算术 (2 + 3) * 4 / 2 - 1 = 9")
    void tc01_arithmetic() {
        Object result = eval("(2 + 3) * 4 / 2 - 1");
        assertThat(((Number) result).doubleValue()).isEqualTo(9.0);
    }

    // ========================= 2. 比较运算 =========================

    @Test
    @DisplayName("TC-E02: 比较运算 返回布尔值")
    void tc02_comparison() {
        assertThat(eval("5 > 3")).isEqualTo(true);
        assertThat(eval("5 < 3")).isEqualTo(false);
        assertThat(eval("3 == 3")).isEqualTo(true);
        assertThat(eval("3 != 4")).isEqualTo(true);
    }

    // ========================= 3. 字符串拼接 =========================

    @Test
    @DisplayName("TC-E03: 字符串拼接 + 运算符")
    void tc03_stringConcat() {
        Map<String, Object> row = Map.of("firstName", "张", "lastName", "三");
        Object result = eval("{firstName} + {lastName}", row);
        assertThat(result).isEqualTo("张三");
    }

    // ========================= 4. IF 函数 =========================

    @Test
    @DisplayName("TC-E04: IF 函数条件分支")
    void tc04_ifFunction() {
        Map<String, Object> row1 = Map.of("score", 90);
        Map<String, Object> row2 = Map.of("score", 50);
        assertThat(eval("IF({score} >= 60, \"及格\", \"不及格\")", row1)).isEqualTo("及格");
        assertThat(eval("IF({score} >= 60, \"及格\", \"不及格\")", row2)).isEqualTo("不及格");
    }

    // ========================= 5. SUM 多参 =========================

    @Test
    @DisplayName("TC-E05: SUM 多参数求和")
    void tc05_sumMultiArgs() {
        Map<String, Object> row = new HashMap<>();
        row.put("a", 10);
        row.put("b", 20);
        row.put("c", null);  // null 应被忽略
        Object result = eval("SUM({a}, {b}, {c}, 5)", row);
        assertThat(((Number) result).doubleValue()).isEqualTo(35.0);
    }

    // ========================= 6. null 安全 =========================

    @Test
    @DisplayName("TC-E06: null 值在算术中视为 0，在字符串中视为空串")
    void tc06_nullSafe() {
        Map<String, Object> row = new HashMap<>();
        row.put("val", null);
        // null + 5 = 0 + 5 = 5
        assertThat(((Number) eval("{val} + 5", row)).doubleValue()).isEqualTo(5.0);
        // CONCAT(null, "ok") = "" + "ok"
        assertThat(eval("CONCAT({val}, \"ok\")", row)).isEqualTo("ok");
    }

    // ========================= 7. 缺失字段返回 null =========================

    @Test
    @DisplayName("TC-E07: 引用不存在的字段 key 返回 null，不抛异常")
    void tc07_missingField() {
        // row 为空，{missing} 应返回 null（不抛出异常）
        Map<String, Object> emptyRow = Collections.emptyMap();
        Node ast = new FormulaParser("{missing}").parse();
        Object result = new FormulaEvaluator(emptyRow, Collections.emptyList()).evaluate(ast);
        assertThat(result).isNull();
    }

    // ========================= 8. 循环引用检测（深度上限） =========================

    @Test
    @DisplayName("TC-E08: 超深嵌套三元触发深度上限返回 #ERROR!")
    void tc08_depthLimit() {
        // 手动构造一个超过 MAX_DEPTH(10) 层嵌套的 AST
        // 通过反复包裹三元来触发深度检测
        // 简单方法：构造 11 层嵌套括号 + IF
        StringBuilder expr = new StringBuilder();
        for (int i = 0; i < 12; i++) expr.insert(0, "IF(true, ");
        expr.append("1");
        for (int i = 0; i < 12; i++) expr.append(", 0)");

        Node ast = new FormulaParser(expr.toString()).parse();
        Object result = new FormulaEvaluator(Collections.emptyMap(), Collections.emptyList()).evaluate(ast);
        // 超深度时 evaluate 捕获异常返回 #ERROR!
        assertThat(result).isEqualTo("#ERROR!");
    }
}
