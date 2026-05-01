package com.pengcheng.realty.sop.template;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SOP 文档模板渲染器
 * <p>
 * 使用 Hutool {@link StrUtil#format(CharSequence, Map)} 将 HTML 模板中的
 * {@code {{var}}} 双花括号占位符替换为实际值。
 * <ul>
 *   <li>缺失的变量：保留原始占位符，不抛异常</li>
 *   <li>特殊字符（&lt; &gt; &amp;）：不做 HTML 转义，由调用方自行处理</li>
 *   <li>不支持嵌套占位符（如 {{{{var}}}}}），视为普通文本</li>
 * </ul>
 */
@Component
public class SopTemplateRenderer {

    /**
     * 占位符模式：{{key}}，key 不能含有 {} 字符，确保单次扫描不会递归解析嵌套花括号。
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^{}]+?)\\}\\}");

    /**
     * 渲染 HTML 模板，将 {{key}} 替换为 vars 中对应的值。
     * <p>使用单次正则扫描，避免 HashMap 迭代顺序导致 {{{{x}}}} 类嵌套占位符被意外递归解析。
     *
     * @param templateHtml 含 {{var}} 占位符的 HTML 字符串
     * @param vars         变量 key -> value 映射
     * @return 渲染后的 HTML 字符串
     */
    public String render(String templateHtml, Map<String, String> vars) {
        if (templateHtml == null || templateHtml.isBlank()) {
            return "";
        }
        if (vars == null || vars.isEmpty()) {
            return templateHtml;
        }
        Matcher m = PLACEHOLDER.matcher(templateHtml);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1).trim();
            String value = vars.get(key);
            // 缺失变量：保留原始占位符；命中：替换为对应值
            String repl = value != null ? value : m.group();
            m.appendReplacement(out, Matcher.quoteReplacement(repl));
        }
        m.appendTail(out);
        return out.toString();
    }
}
