package com.pengcheng.common.i18n;

import java.util.Locale;

/**
 * 当前请求的 Locale ThreadLocal（V4 Phase 6 L5）。
 *
 * <p>由 LocaleInterceptor 进入请求时设置，业务代码可读取做本地化决策。
 */
public final class LocaleContextHolder {

    private static final ThreadLocal<Locale> CURRENT = new ThreadLocal<>();
    public static final Locale DEFAULT = Locale.SIMPLIFIED_CHINESE;

    private LocaleContextHolder() {}

    public static void set(Locale locale) {
        CURRENT.set(locale != null ? locale : DEFAULT);
    }

    public static Locale get() {
        Locale l = CURRENT.get();
        return l != null ? l : DEFAULT;
    }

    public static String tag() {
        return get().toLanguageTag();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
