package com.pengcheng.system.smarttable.formula;

import java.util.List;

/**
 * 公式 AST 节点定义
 * 支持字面量、字段引用、二元/一元/三元运算及函数调用
 */
public final class FormulaAst {

    private FormulaAst() {}

    /** 所有 AST 节点的公共接口 */
    public interface Node {}

    // ========================= 字面量 =========================

    /** 数字字面量（统一用 Double 存储） */
    public static final class NumberLiteral implements Node {
        public final double value;
        public NumberLiteral(double value) { this.value = value; }
    }

    /** 字符串字面量 */
    public static final class StringLiteral implements Node {
        public final String value;
        public StringLiteral(String value) { this.value = value; }
    }

    /** 布尔字面量 */
    public static final class BooleanLiteral implements Node {
        public final boolean value;
        public BooleanLiteral(boolean value) { this.value = value; }
    }

    /** null 字面量 */
    public static final class NullLiteral implements Node {
        public static final NullLiteral INSTANCE = new NullLiteral();
        private NullLiteral() {}
    }

    // ========================= 字段引用 =========================

    /** 字段引用节点，如 {fieldKey} */
    public static final class FieldRefNode implements Node {
        public final String fieldKey;
        public FieldRefNode(String fieldKey) { this.fieldKey = fieldKey; }
    }

    // ========================= 运算符节点 =========================

    /** 二元运算节点：left op right */
    public static final class BinaryOpNode implements Node {
        public final String op;   // + - * / % == != > >= < <= && ||
        public final Node left;
        public final Node right;
        public BinaryOpNode(String op, Node left, Node right) {
            this.op = op; this.left = left; this.right = right;
        }
    }

    /** 一元运算节点：! 或 - */
    public static final class UnaryOpNode implements Node {
        public final String op;   // ! or -
        public final Node operand;
        public UnaryOpNode(String op, Node operand) {
            this.op = op; this.operand = operand;
        }
    }

    /** 三元节点：condition ? thenExpr : elseExpr */
    public static final class TernaryNode implements Node {
        public final Node condition;
        public final Node thenExpr;
        public final Node elseExpr;
        public TernaryNode(Node condition, Node thenExpr, Node elseExpr) {
            this.condition = condition; this.thenExpr = thenExpr; this.elseExpr = elseExpr;
        }
    }

    /** 函数调用节点：name(args...) */
    public static final class FunctionCallNode implements Node {
        public final String name;       // 大写规范化后的函数名
        public final List<Node> args;
        public FunctionCallNode(String name, List<Node> args) {
            this.name = name; this.args = args;
        }
    }
}
