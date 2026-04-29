package com.pengcheng.config;

import com.pengcheng.common.i18n.LocaleInterceptor;
import com.pengcheng.common.i18n.LocaleResolver;
import com.pengcheng.system.i18n.service.DbLocaleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * i18n WebMvc 配置。
 *
 * <p>职责：
 * <ol>
 *   <li>将 {@link DbLocaleResolver}（用户偏好 DB → Accept-Language → zh-CN）注册为
 *       Spring Bean（若下游已注册 {@link LocaleResolver}，此处 @ConditionalOnMissingBean
 *       可按需添加）；</li>
 *   <li>注册 {@link LocaleInterceptor} 到 /admin/** 和 /app/** 请求路径，
 *       在每个请求进入时设置 {@code LocaleContextHolder}，结束时自动清理。</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class I18nWebMvcConfig implements WebMvcConfigurer {

    private final DbLocaleResolver dbLocaleResolver;

    /**
     * 将 DbLocaleResolver 暴露为 LocaleResolver 接口 Bean，
     * 方便其他组件（如 MessageSourceAccessor）按接口注入。
     */
    @Bean
    public LocaleResolver localeResolver() {
        return dbLocaleResolver;
    }

    /**
     * 注册 LocaleInterceptor：
     * <ul>
     *   <li>/admin/** — 管理端接口</li>
     *   <li>/app/**  — 应用端接口</li>
     *   <li>/api/i18n/** — 前端词条拉取接口（保证词条接口本身也能正确识别 locale）</li>
     * </ul>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LocaleInterceptor(dbLocaleResolver))
                .addPathPatterns("/admin/**", "/app/**", "/api/i18n/**");
    }
}
