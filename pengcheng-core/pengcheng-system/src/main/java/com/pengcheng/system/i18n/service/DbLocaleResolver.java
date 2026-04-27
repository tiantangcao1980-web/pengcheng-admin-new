package com.pengcheng.system.i18n.service;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.i18n.LocaleContextHolder;
import com.pengcheng.common.i18n.LocaleResolver;
import com.pengcheng.system.i18n.entity.UserLocalePreference;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 默认 LocaleResolver：用户偏好（DB） > Accept-Language 头 > zh-CN 默认。
 */
@Component
@RequiredArgsConstructor
public class DbLocaleResolver implements LocaleResolver {

    private final UserLocalePreferenceService prefService;

    @Override
    public Locale resolve(HttpServletRequest request) {
        try {
            if (StpUtil.isLogin()) {
                Long uid = StpUtil.getLoginIdAsLong();
                UserLocalePreference pref = prefService.get(uid);
                if (pref != null && pref.getLocale() != null) {
                    return Locale.forLanguageTag(pref.getLocale());
                }
            }
        } catch (Exception ignored) {
            // 未登录 / Sa-Token 未就绪 — 走下一档
        }
        Locale fromHeader = LocaleResolver.parseAcceptLanguage(request.getHeader("Accept-Language"));
        return fromHeader != null ? fromHeader : LocaleContextHolder.DEFAULT;
    }
}
