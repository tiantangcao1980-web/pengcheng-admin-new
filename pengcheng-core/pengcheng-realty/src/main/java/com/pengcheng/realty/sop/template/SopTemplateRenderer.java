package com.pengcheng.realty.sop.template;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

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
     * 渲染 HTML 模板，将 {{key}} 替换为 vars 中对应的值。
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
        // hutool StrUtil.format 使用 {} 单花括号，不直接支持 {{}}。
        // 此处手动替换双花括号：遍历 vars，将 {{key}} -> value。
        String result = templateHtml;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
