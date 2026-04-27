package com.pengcheng.realty.sop;

import com.pengcheng.realty.sop.template.SopTemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SopTemplateRenderer 单元测试（4 用例）
 */
class SopTemplateRendererTest {

    private SopTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new SopTemplateRenderer();
    }

    /**
     * 用例 1：正常占位符替换
     */
    @Test
    @DisplayName("正常占位符替换：所有 {{key}} 均被替换为对应值")
    void testNormalReplacement() {
        String template = "<p>客户：{{customer_name}}，楼盘：{{project_name}}</p>";
        Map<String, String> vars = new HashMap<>();
        vars.put("customer_name", "张三");
        vars.put("project_name", "江湾华庭");

        String result = renderer.render(template, vars);

        assertThat(result).isEqualTo("<p>客户：张三，楼盘：江湾华庭</p>");
    }

    /**
     * 用例 2：缺失变量保留原样（不抛异常，不替换为空）
     */
    @Test
    @DisplayName("缺失变量保留原始占位符：vars 中没有对应 key 时 {{key}} 原样保留")
    void testMissingVariableKeptAsIs() {
        String template = "<p>客户：{{customer_name}}，销售：{{salesperson_name}}</p>";
        Map<String, String> vars = new HashMap<>();
        vars.put("customer_name", "李四");
        // salesperson_name 未提供

        String result = renderer.render(template, vars);

        assertThat(result).isEqualTo("<p>客户：李四，销售：{{salesperson_name}}</p>");
        assertThat(result).contains("{{salesperson_name}}");
    }

    /**
     * 用例 3：含特殊字符（<>& 等），不做 HTML 转义
     */
    @Test
    @DisplayName("特殊字符不转义：值中含 < > & 字符时原样写入")
    void testSpecialCharactersNotEscaped() {
        String template = "<p>备注：{{remark}}</p>";
        Map<String, String> vars = new HashMap<>();
        vars.put("remark", "价格 < 300万 & 面积 > 80m²");

        String result = renderer.render(template, vars);

        assertThat(result).contains("价格 < 300万 & 面积 > 80m²");
        // 原始 HTML 标签结构不破坏
        assertThat(result).startsWith("<p>备注：");
        assertThat(result).endsWith("</p>");
    }

    /**
     * 用例 4：嵌套占位符不支持（视为普通文本，不递归解析）
     */
    @Test
    @DisplayName("嵌套占位符不支持：{{{{key}}}} 不会被递归解析")
    void testNestedPlaceholderNotSupported() {
        // 模板中有 {{{{inner}}}} —— 实际为 {{ + {{inner}} + }}，不是合法占位符
        String template = "<p>嵌套：{{{{inner}}}}</p>";
        Map<String, String> vars = new HashMap<>();
        vars.put("inner", "值");
        vars.put("{{inner}}", "不该被替换");

        // 行为：{{inner}} 替换为 "值"，外层 {{ 和 }} 原样保留
        // 即 {{{{inner}}}} -> {{值}}
        String result = renderer.render(template, vars);

        // 外层双花括号残留，不会再做二次解析
        assertThat(result).contains("{{值}}");
    }
}
