package com.pengcheng.system.i18n.service;

import com.pengcheng.common.i18n.LocaleContextHolder;
import com.pengcheng.system.i18n.entity.I18nMessage;
import com.pengcheng.system.i18n.mapper.I18nMessageMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * i18n 词条服务。
 *
 * <p>启动时全量加载所有 locale 的词条到内存（ConcurrentMap），运行时编辑后调
 * {@link #refresh()} 清空缓存重载。
 *
 * <p>fallback 链：当前 locale → en-US → zh-CN → 返回 namespace.key 本身。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class I18nMessageService {

    private final I18nMessageMapper mapper;

    /** locale → ("namespace.key" → value) */
    private final ConcurrentMap<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            refresh();
        } catch (Exception e) {
            log.warn("[i18n] 启动加载词条失败（DB 可能未就绪），稍后请手动 /refresh-cache: {}", e.getMessage());
        }
    }

    public synchronized void refresh() {
        cache.clear();
        List<I18nMessage> all = mapper.selectList(null);
        for (I18nMessage m : all) {
            cache.computeIfAbsent(m.getLocale(), k -> new HashMap<>())
                 .put(m.getNamespace() + "." + m.getKeyName(), m.getValueText());
        }
        log.info("[i18n] 缓存刷新：{} locales", cache.size());
    }

    public String resolve(String namespace, String key, Object... args) {
        return resolve(namespace, key, LocaleContextHolder.get(), args);
    }

    public String resolve(String namespace, String key, Locale locale, Object... args) {
        String fullKey = namespace + "." + key;
        String tag = locale != null ? locale.toLanguageTag() : "zh-CN";

        String value = lookup(tag, fullKey);
        if (value == null && !"en-US".equals(tag)) value = lookup("en-US", fullKey);
        if (value == null && !"zh-CN".equals(tag)) value = lookup("zh-CN", fullKey);
        if (value == null) value = fullKey;

        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(value, args);
            } catch (Exception e) {
                return value;
            }
        }
        return value;
    }

    private String lookup(String localeTag, String fullKey) {
        Map<String, String> map = cache.get(localeTag);
        return map != null ? map.get(fullKey) : null;
    }

    /** 导出指定 locale 的全量词条（前端启动时拉一次）。 */
    public Map<String, String> exportLocale(String localeTag) {
        Map<String, String> map = cache.get(localeTag);
        return map != null ? new HashMap<>(map) : new HashMap<>();
    }
}
