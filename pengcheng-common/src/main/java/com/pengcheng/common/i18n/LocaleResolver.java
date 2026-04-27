package com.pengcheng.common.i18n;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

/**
 * Locale 解析策略。优先级：用户偏好 → cookie/header Accept-Language → 默认。
 */
public interface LocaleResolver {

    Locale resolve(HttpServletRequest request);

    /** 标准 Accept-Language 头解析（fallback）。 */
    static Locale parseAcceptLanguage(String header) {
        if (header == null || header.isBlank()) return LocaleContextHolder.DEFAULT;
        String first = header.split(",")[0].trim();
        // 去除 q=...
        int semi = first.indexOf(';');
        if (semi > 0) first = first.substring(0, semi);
        try {
            return Locale.forLanguageTag(first.replace('_', '-'));
        } catch (Exception e) {
            return LocaleContextHolder.DEFAULT;
        }
    }
}
