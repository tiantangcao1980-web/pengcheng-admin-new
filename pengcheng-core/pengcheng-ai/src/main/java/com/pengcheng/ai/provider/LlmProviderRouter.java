package com.pengcheng.ai.provider;

import com.pengcheng.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM Provider 路由器
 *
 * 选择策略（优先级从高到低）：
 *   1. 显式 providerName 匹配（如调用 generate("ollama", ...)）
 *   2. AiProperties.provider 配置值（默认 "auto"）
 *   3. 第一个 isAvailable() 为 true 的 provider
 *
 * 故障转移：默认选择不可用时，自动 fallback 到下一个可用 provider。
 * 私有化部署场景：配置 provider=ollama 强制走本地，敏感数据不出公司。
 */
@Slf4j
@Component
public class LlmProviderRouter {

    private final Map<String, LlmProvider> providers;
    private final List<LlmProvider> orderedProviders;
    private final AiProperties properties;

    public LlmProviderRouter(List<LlmProvider> providerBeans, AiProperties properties) {
        this.providers = providerBeans.stream()
                .collect(Collectors.toMap(LlmProvider::name, p -> p));
        this.orderedProviders = providerBeans;
        this.properties = properties;
        log.info("[LlmProviderRouter] 已注册 Provider: {}", providers.keySet());
    }

    /** 获取按配置选择的默认 Provider */
    public LlmProvider resolve() {
        return resolve(null);
    }

    /**
     * 选择 Provider：
     *   - 指定 name → 命中且可用则返回
     *   - 配置 provider != "auto" → 命中且可用则返回
     *   - 否则取第一个可用
     *   - 全部不可用时返回第一个（让调用方自行处理异常 / fallback）
     */
    public LlmProvider resolve(String preferName) {
        // 1. 显式指定优先
        if (preferName != null && !preferName.isBlank()) {
            LlmProvider p = providers.get(preferName);
            if (p != null && p.isAvailable()) return p;
            log.warn("[LlmProviderRouter] 指定 Provider {} 不可用，走配置 fallback", preferName);
        }

        // 2. 配置项
        String configured = properties.getProvider();
        if (configured != null && !"auto".equalsIgnoreCase(configured)) {
            LlmProvider p = providers.get(configured);
            if (p != null && p.isAvailable()) return p;
            log.warn("[LlmProviderRouter] 配置 Provider {} 不可用，走自动选择", configured);
        }

        // 3. 第一个可用
        for (LlmProvider p : orderedProviders) {
            if (p.isAvailable()) return p;
        }

        // 4. 全部不可用：返回第一个，由其降级
        log.error("[LlmProviderRouter] 所有 Provider 均不可用，返回首个 {}",
                orderedProviders.isEmpty() ? "<none>" : orderedProviders.get(0).name());
        return orderedProviders.isEmpty() ? null : orderedProviders.get(0);
    }

    /** 直接生成（按配置选 Provider） */
    public String generate(String systemPrompt, String userInput) {
        LlmProvider p = resolve();
        if (p == null) {
            return "[LLM 路由失败] 未配置任何 Provider";
        }
        return p.generate(systemPrompt, userInput);
    }

    /** 指定 Provider 生成 */
    public String generate(String preferName, String systemPrompt, String userInput) {
        LlmProvider p = resolve(preferName);
        if (p == null) {
            return "[LLM 路由失败] 未配置任何 Provider";
        }
        return p.generate(systemPrompt, userInput);
    }
}
