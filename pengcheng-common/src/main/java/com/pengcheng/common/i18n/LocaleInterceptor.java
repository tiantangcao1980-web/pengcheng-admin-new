package com.pengcheng.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

/**
 * 进入请求时设置 LocaleContextHolder；afterCompletion 清理 ThreadLocal。
 *
 * <p>注意：本类不带 @Component，由 starter 端 WebMvcConfigurer 显式注册，
 * 注入具体的 {@link LocaleResolver} 实现。
 */
public class LocaleInterceptor implements HandlerInterceptor {

    private final LocaleResolver resolver;

    public LocaleInterceptor(LocaleResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        Locale locale = resolver != null ? resolver.resolve(req) : LocaleContextHolder.DEFAULT;
        LocaleContextHolder.set(locale);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        LocaleContextHolder.clear();
    }
}
