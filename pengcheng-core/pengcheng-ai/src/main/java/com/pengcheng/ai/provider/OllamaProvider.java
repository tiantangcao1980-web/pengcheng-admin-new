package com.pengcheng.ai.provider;

import com.pengcheng.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ollama 本地 LLM Provider
 *
 * 通过 HTTP 直调 Ollama 服务（默认 http://localhost:11434/api/generate）。
 * 不依赖 spring-ai-ollama-starter，避免与 dashscope-starter 的 ChatModel Bean 冲突。
 *
 * 用途：
 *   - 私有化部署：客户数据不出公司
 *   - 成本敏感：本地推理无 token 费用
 *   - 离线开发：无网络环境调试
 *
 * 模型选型建议（按硬件）：
 *   - 8GB 显存：qwen2:7b / deepseek-llm:7b
 *   - 16GB 显存：qwen2:14b / deepseek-coder:33b-int4
 *   - 24GB+：qwen2:72b / llama3:70b
 */
@Slf4j
@Component
public class OllamaProvider implements LlmProvider {

    private final AiProperties properties;
    private final RestClient restClient;
    private volatile Boolean cachedAvailable;

    public OllamaProvider(AiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getOllamaBaseUrl())
                .build();
    }

    @Override
    public String name() {
        return "ollama";
    }

    /**
     * 通过尝试 GET /api/tags（列出本地模型）判定可用性。
     * 结果缓存避免每次调用都探活。
     */
    @Override
    public boolean isAvailable() {
        if (cachedAvailable != null) return cachedAvailable;
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            cachedAvailable = true;
            log.info("[OllamaProvider] 探活成功 baseUrl={}", properties.getOllamaBaseUrl());
        } catch (RestClientException e) {
            cachedAvailable = false;
            log.debug("[OllamaProvider] 探活失败 baseUrl={}: {}",
                    properties.getOllamaBaseUrl(), e.getMessage());
        }
        return cachedAvailable;
    }

    @Override
    public String generate(String systemPrompt, String userInput) {
        if (!StringUtils.hasText(userInput)) return "";

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", properties.getOllamaModel());
        req.put("stream", false);
        req.put("prompt", userInput);
        if (StringUtils.hasText(systemPrompt)) {
            req.put("system", systemPrompt);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri("/api/generate")
                    .body(req)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) return "[Ollama 返回空]";
            Object content = resp.get("response");
            return content == null ? "" : content.toString();
        } catch (Exception e) {
            log.warn("[OllamaProvider] 调用失败 model={}: {}",
                    properties.getOllamaModel(), e.getMessage());
            return "[Ollama 调用失败] " + e.getMessage();
        }
    }

    /** 测试用：清缓存重新探活 */
    public void resetAvailability() {
        this.cachedAvailable = null;
    }

    /** 测试用：超时配置 */
    @SuppressWarnings("unused")
    private Duration timeout() {
        return Duration.ofSeconds(properties.getOllamaTimeoutSeconds());
    }
}
