package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.entity.SmartTableField;
import com.pengcheng.system.smarttable.formula.FormulaAst.Node;

import java.util.List;
import java.util.Map;

/**
 * 公式服务接口
 *
 * compile()   — 将表达式字符串解析为 AST（带缓存）
 * evaluate()  — 给定行数据执行公式，返回计算结果（异常时返回 "#ERROR!"）
 * validate()  — 仅做语法校验，不求值
 */
public interface FormulaService {

    /**
     * 编译（解析）公式表达式，结果缓存复用
     *
     * @param expr 公式字符串，如 "{amount} * 1.1 + IF({qty} > 0, {discount}, 0)"
     * @return AST 根节点
     * @throws FormulaParser.FormulaParseException 语法错误
     */
    Node compile(String expr);

    /**
     * 对给定行数据求值
     *
     * @param expr   公式字符串
     * @param row    行数据 Map（key = fieldKey, value = 字段值）
     * @param fields 字段元信息列表（用于类型提示）
     * @return 计算结果，异常时返回 "#ERROR!"
     */
    Object evaluate(String expr, Map<String, Object> row, List<SmartTableField> fields);

    /**
     * 语法校验
     *
     * @param expr 公式字符串
     * @return true = 语法合法；false = 存在语法错误
     */
    boolean validate(String expr);
}
