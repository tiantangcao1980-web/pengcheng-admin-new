package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.formula.FormulaAst.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 公式解析器（递归下降 / Pratt 混合）
 *
 * 支持的语法：
 *   字面量  : number | string | boolean | null
 *   字段引用: {fieldKey}
 *   算术    : + - * / % （带优先级）
 *   比较    : == != > >= < <=
 *   逻辑    : && || !
 *   三元    : expr ? expr : expr
 *   函数    : NAME(arg, ...)
 *   括号    : (expr)
 *
 * 运算符优先级（从低到高）：
 *   1  三元 ?:
 *   2  ||
 *   3  &&
 *   4  == !=
 *   5  > >= < <=
 *   6  + -
 *   7  * / %
 *   8  一元 ! -
 *   9  原子：字面量 / 字段引用 / 函数调用 / 括号
 */
public class FormulaParser {

    // ========================= Tokenizer =========================

    private enum TokenType {
        NUMBER, STRING, BOOLEAN, NULL,
        IDENT,          // 函数名 / 关键字
        FIELD_REF,      // {key}
        PLUS, MINUS, STAR, SLASH, PERCENT,
        EQ, NEQ, GT, GTE, LT, LTE,
        AND, OR, BANG,
        QUESTION, COLON,
        LPAREN, RPAREN,
        COMMA,
        EOF
    }

    private static final class Token {
        final TokenType type;
        final String raw;
        Token(TokenType type, String raw) { this.type = type; this.raw = raw; }
        @Override public String toString() { return type + "(" + raw + ")"; }
    }

    // ========================= 词法分析 =========================

    private final String src;
    private int pos;
    private final List<Token> tokens = new ArrayList<>();
    private int cur;

    public FormulaParser(String expression) {
        this.src = expression == null ? "" : expression;
        this.pos = 0;
        tokenize();
        this.cur = 0;
    }

    private void tokenize() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) { pos++; continue; }

            // 字段引用 {key}
            if (c == '{') {
                int start = ++pos;
                while (pos < src.length() && src.charAt(pos) != '}') pos++;
                tokens.add(new Token(TokenType.FIELD_REF, src.substring(start, pos)));
                if (pos < src.length()) pos++; // 跳过 }
                continue;
            }

            // 字符串
            if (c == '"' || c == '\'') {
                char quote = c;
                pos++;
                StringBuilder sb = new StringBuilder();
                while (pos < src.length() && src.charAt(pos) != quote) {
                    if (src.charAt(pos) == '\\' && pos + 1 < src.length()) {
                        pos++;
                        char esc = src.charAt(pos);
                        switch (esc) {
                            case 'n': sb.append('\n'); break;
                            case 't': sb.append('\t'); break;
                            default:  sb.append(esc);  break;
                        }
                    } else {
                        sb.append(src.charAt(pos));
                    }
                    pos++;
                }
                if (pos < src.length()) pos++;
                tokens.add(new Token(TokenType.STRING, sb.toString()));
                continue;
            }

            // 数字
            if (Character.isDigit(c) || (c == '.' && pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1)))) {
                int start = pos;
                while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
                tokens.add(new Token(TokenType.NUMBER, src.substring(start, pos)));
                continue;
            }

            // 标识符 / 关键字
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
                String word = src.substring(start, pos);
                if ("true".equalsIgnoreCase(word))       tokens.add(new Token(TokenType.BOOLEAN, "true"));
                else if ("false".equalsIgnoreCase(word)) tokens.add(new Token(TokenType.BOOLEAN, "false"));
                else if ("null".equalsIgnoreCase(word))  tokens.add(new Token(TokenType.NULL, "null"));
                else                                      tokens.add(new Token(TokenType.IDENT, word));
                continue;
            }

            // 双字符运算符
            if (pos + 1 < src.length()) {
                String two = src.substring(pos, pos + 2);
                switch (two) {
                    case "==": tokens.add(new Token(TokenType.EQ,  "==")); pos += 2; continue;
                    case "!=": tokens.add(new Token(TokenType.NEQ, "!=")); pos += 2; continue;
                    case ">=": tokens.add(new Token(TokenType.GTE, ">=")); pos += 2; continue;
                    case "<=": tokens.add(new Token(TokenType.LTE, "<=")); pos += 2; continue;
                    case "&&": tokens.add(new Token(TokenType.AND, "&&")); pos += 2; continue;
                    case "||": tokens.add(new Token(TokenType.OR,  "||")); pos += 2; continue;
                }
            }

            // 单字符
            switch (c) {
                case '+': tokens.add(new Token(TokenType.PLUS,    "+")); break;
                case '-': tokens.add(new Token(TokenType.MINUS,   "-")); break;
                case '*': tokens.add(new Token(TokenType.STAR,    "*")); break;
                case '/': tokens.add(new Token(TokenType.SLASH,   "/")); break;
                case '%': tokens.add(new Token(TokenType.PERCENT, "%")); break;
                case '>': tokens.add(new Token(TokenType.GT,      ">")); break;
                case '<': tokens.add(new Token(TokenType.LT,      "<")); break;
                case '!': tokens.add(new Token(TokenType.BANG,    "!")); break;
                case '?': tokens.add(new Token(TokenType.QUESTION,"?")); break;
                case ':': tokens.add(new Token(TokenType.COLON,   ":")); break;
                case '(': tokens.add(new Token(TokenType.LPAREN,  "(")); break;
                case ')': tokens.add(new Token(TokenType.RPAREN,  ")")); break;
                case ',': tokens.add(new Token(TokenType.COMMA,   ",")); break;
                default:  /* 跳过非法字符 */ break;
            }
            pos++;
        }
        tokens.add(new Token(TokenType.EOF, ""));
    }

    // ========================= 工具方法 =========================

    private Token peek() { return tokens.get(cur); }
    private Token advance() { return tokens.get(cur++); }
    private boolean check(TokenType t) { return peek().type == t; }
    private boolean match(TokenType t) {
        if (check(t)) { advance(); return true; }
        return false;
    }
    private Token expect(TokenType t) {
        if (!check(t)) throw new FormulaParseException("期望 " + t + " 但遇到 " + peek());
        return advance();
    }

    // ========================= 解析入口 =========================

    /**
     * 解析整个表达式，返回 AST 根节点
     */
    public Node parse() {
        Node node = parseExpression();
        if (!check(TokenType.EOF)) {
            throw new FormulaParseException("表达式未完整解析，剩余 token: " + peek());
        }
        return node;
    }

    // 层级 1：三元 ?:
    private Node parseExpression() {
        Node node = parseOr();
        if (match(TokenType.QUESTION)) {
            Node thenExpr = parseExpression();
            expect(TokenType.COLON);
            Node elseExpr = parseExpression();
            return new TernaryNode(node, thenExpr, elseExpr);
        }
        return node;
    }

    // 层级 2：||
    private Node parseOr() {
        Node left = parseAnd();
        while (check(TokenType.OR)) {
            String op = advance().raw;
            left = new BinaryOpNode(op, left, parseAnd());
        }
        return left;
    }

    // 层级 3：&&
    private Node parseAnd() {
        Node left = parseEquality();
        while (check(TokenType.AND)) {
            String op = advance().raw;
            left = new BinaryOpNode(op, left, parseEquality());
        }
        return left;
    }

    // 层级 4：== !=
    private Node parseEquality() {
        Node left = parseComparison();
        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            String op = advance().raw;
            left = new BinaryOpNode(op, left, parseComparison());
        }
        return left;
    }

    // 层级 5：> >= < <=
    private Node parseComparison() {
        Node left = parseAddSub();
        while (check(TokenType.GT) || check(TokenType.GTE) || check(TokenType.LT) || check(TokenType.LTE)) {
            String op = advance().raw;
            left = new BinaryOpNode(op, left, parseAddSub());
        }
        return left;
    }

    // 层级 6：+ -
    private Node parseAddSub() {
        Node left = parseMulDiv();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = advance().raw;
            left = new BinaryOpNode(op, left, parseMulDiv());
        }
        return left;
    }

    // 层级 7：* / %
    private Node parseMulDiv() {
        Node left = parseUnary();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            String op = advance().raw;
            left = new BinaryOpNode(op, left, parseUnary());
        }
        return left;
    }

    // 层级 8：一元 ! -
    private Node parseUnary() {
        if (check(TokenType.BANG)) {
            advance();
            return new UnaryOpNode("!", parseUnary());
        }
        if (check(TokenType.MINUS)) {
            advance();
            return new UnaryOpNode("-", parseUnary());
        }
        return parseAtom();
    }

    // 层级 9：原子
    private Node parseAtom() {
        Token t = peek();

        // 括号
        if (t.type == TokenType.LPAREN) {
            advance();
            Node inner = parseExpression();
            expect(TokenType.RPAREN);
            return inner;
        }

        // 字面量
        if (t.type == TokenType.NUMBER) {
            advance();
            return new NumberLiteral(Double.parseDouble(t.raw));
        }
        if (t.type == TokenType.STRING) {
            advance();
            return new StringLiteral(t.raw);
        }
        if (t.type == TokenType.BOOLEAN) {
            advance();
            return new BooleanLiteral("true".equals(t.raw));
        }
        if (t.type == TokenType.NULL) {
            advance();
            return NullLiteral.INSTANCE;
        }

        // 字段引用
        if (t.type == TokenType.FIELD_REF) {
            advance();
            return new FieldRefNode(t.raw);
        }

        // 函数调用 or 布尔关键词（已在词法层处理）
        if (t.type == TokenType.IDENT) {
            advance();
            if (check(TokenType.LPAREN)) {
                advance();
                List<Node> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    args.add(parseExpression());
                    while (match(TokenType.COMMA)) {
                        args.add(parseExpression());
                    }
                }
                expect(TokenType.RPAREN);
                return new FunctionCallNode(t.raw.toUpperCase(), args);
            }
            // 裸标识符：当作字段引用（兼容不带花括号写法）
            return new FieldRefNode(t.raw);
        }

        throw new FormulaParseException("意外的 token: " + t);
    }

    // ========================= 异常 =========================

    public static final class FormulaParseException extends RuntimeException {
        public FormulaParseException(String msg) { super(msg); }
    }
}
