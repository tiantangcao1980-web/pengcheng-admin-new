package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.formula.FormulaAst.*;
import com.pengcheng.system.smarttable.formula.FormulaParser.FormulaParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * FormulaParser 单元测试 — 12 用例
 *
 * 覆盖：字面量、二元运算、嵌套括号、字段引用、6 个内置函数、
 *       错误恢复、运算符优先级
 */
@DisplayName("FormulaParser — 解析器测试")
class FormulaParserTest {

    private Node parse(String expr) {
        return new FormulaParser(expr).parse();
    }

    // ========================= 1. 数字字面量 =========================

    @Test
    @DisplayName("TC-01: 整数字面量解析")
    void tc01_integerLiteral() {
        Node node = parse("42");
        assertThat(node).isInstanceOf(NumberLiteral.class);
        assertThat(((NumberLiteral) node).value).isEqualTo(42.0);
    }

    // ========================= 2. 字符串字面量 =========================

    @Test
    @DisplayName("TC-02: 字符串字面量解析（双引号）")
    void tc02_stringLiteral() {
        Node node = parse("\"hello world\"");
        assertThat(node).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) node).value).isEqualTo("hello world");
    }

    // ========================= 3. 布尔字面量 =========================

    @Test
    @DisplayName("TC-03: 布尔字面量 true/false")
    void tc03_booleanLiteral() {
        assertThat(parse("true")).isInstanceOf(BooleanLiteral.class);
        assertThat(((BooleanLiteral) parse("true")).value).isTrue();
        assertThat(((BooleanLiteral) parse("false")).value).isFalse();
    }

    // ========================= 4. 二元算术运算 =========================

    @Test
    @DisplayName("TC-04: 二元运算 + 和 *")
    void tc04_binaryArithmetic() {
        Node node = parse("1 + 2 * 3");
        // 优先级：应解析为 1 + (2*3)
        assertThat(node).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode add = (BinaryOpNode) node;
        assertThat(add.op).isEqualTo("+");
        assertThat(add.left).isInstanceOf(NumberLiteral.class);
        assertThat(add.right).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode mul = (BinaryOpNode) add.right;
        assertThat(mul.op).isEqualTo("*");
    }

    // ========================= 5. 嵌套括号 =========================

    @Test
    @DisplayName("TC-05: 括号改变运算优先级 (1+2)*3")
    void tc05_nestedParentheses() {
        Node node = parse("(1 + 2) * 3");
        assertThat(node).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode mul = (BinaryOpNode) node;
        assertThat(mul.op).isEqualTo("*");
        // 左侧应是括号内的加法
        assertThat(mul.left).isInstanceOf(BinaryOpNode.class);
        assertThat(((BinaryOpNode) mul.left).op).isEqualTo("+");
    }

    // ========================= 6. 字段引用 =========================

    @Test
    @DisplayName("TC-06: 字段引用 {amount} * 1.1")
    void tc06_fieldRef() {
        Node node = parse("{amount} * 1.1");
        assertThat(node).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode mul = (BinaryOpNode) node;
        assertThat(mul.left).isInstanceOf(FieldRefNode.class);
        assertThat(((FieldRefNode) mul.left).fieldKey).isEqualTo("amount");
    }

    // ========================= 7. 内置函数 IF =========================

    @Test
    @DisplayName("TC-07: IF 函数调用解析")
    void tc07_functionIf() {
        Node node = parse("IF({qty} > 0, {price}, 0)");
        assertThat(node).isInstanceOf(FunctionCallNode.class);
        FunctionCallNode fn = (FunctionCallNode) node;
        assertThat(fn.name).isEqualTo("IF");
        assertThat(fn.args).hasSize(3);
        assertThat(fn.args.get(0)).isInstanceOf(BinaryOpNode.class);
    }

    // ========================= 8. 内置函数 SUM =========================

    @Test
    @DisplayName("TC-08: SUM 多参函数解析")
    void tc08_functionSum() {
        Node node = parse("SUM(1, 2, 3, {amount})");
        assertThat(node).isInstanceOf(FunctionCallNode.class);
        assertThat(((FunctionCallNode) node).name).isEqualTo("SUM");
        assertThat(((FunctionCallNode) node).args).hasSize(4);
    }

    // ========================= 9. 内置函数 CONCAT =========================

    @Test
    @DisplayName("TC-09: CONCAT 函数解析")
    void tc09_functionConcat() {
        Node node = parse("CONCAT(\"Hello\", \" \", {name})");
        assertThat(node).isInstanceOf(FunctionCallNode.class);
        assertThat(((FunctionCallNode) node).name).isEqualTo("CONCAT");
        assertThat(((FunctionCallNode) node).args).hasSize(3);
    }

    // ========================= 10. 内置函数 ROUND =========================

    @Test
    @DisplayName("TC-10: ROUND 函数解析（两参）")
    void tc10_functionRound() {
        Node node = parse("ROUND({price}, 2)");
        assertThat(node).isInstanceOf(FunctionCallNode.class);
        FunctionCallNode fn = (FunctionCallNode) node;
        assertThat(fn.name).isEqualTo("ROUND");
        assertThat(fn.args).hasSize(2);
        assertThat(fn.args.get(1)).isInstanceOf(NumberLiteral.class);
        assertThat(((NumberLiteral) fn.args.get(1)).value).isEqualTo(2.0);
    }

    // ========================= 11. 运算符优先级（比较 + 逻辑） =========================

    @Test
    @DisplayName("TC-11: 运算符优先级：&& 高于 ||")
    void tc11_operatorPrecedence() {
        // a || b && c  应解析为 a || (b && c)
        Node node = parse("{a} || {b} && {c}");
        assertThat(node).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode or = (BinaryOpNode) node;
        assertThat(or.op).isEqualTo("||");
        assertThat(or.right).isInstanceOf(BinaryOpNode.class);
        assertThat(((BinaryOpNode) or.right).op).isEqualTo("&&");
    }

    // ========================= 12. 错误恢复 =========================

    @Test
    @DisplayName("TC-12: 语法错误时抛出 FormulaParseException")
    void tc12_parseError() {
        // 未闭合括号
        assertThatThrownBy(() -> parse("(1 + 2"))
                .isInstanceOf(FormulaParseException.class);

        // 空字符串走 validate 路径（通过 Service 层）
        // 直接 new Parser 空串解析结果是 EOF 节点
        assertThatThrownBy(() -> new FormulaParser("1 +").parse())
                .isInstanceOf(FormulaParseException.class);
    }
}
