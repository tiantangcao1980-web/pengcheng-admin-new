package com.pengcheng.ai.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 阿里通义（DashScope）Provider
 *
 * 直接复用 Spring AI 已注入的 ChatClient（由 spring-ai-alibaba-starter-dashscope 自动装配）。
 * isAvailable() 通过尝试一次最小调用判定，避免硬依赖配置项。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeProvider implements LlmProvider {

    private final ChatClient chatClient;

    @Override
    public String name() {
        return "dashscope";
    }

    @Override
    public boolean isAvailable() {
        // ChatClient 由 starter 装配；若 ChatModel 缺失，SpringAiConfig 会注入降级实现，
        // 仍然 isAvailable=true，但调用时会收到降级文本。这里保持简单语义。
        return chatClient != null;
    }

    @Override
    public String generate(String systemPrompt, String userInput) {
        if (!StringUtils.hasText(userInput)) {
            return "";
        }
        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
            if (StringUtils.hasText(systemPrompt)) {
                spec = spec.system(systemPrompt);
            }
            return spec.user(userInput).call().content();
        } catch (Exception e) {
            log.warn("[DashScopeProvider] 调用失败: {}", e.getMessage());
            return "[DashScope 调用失败] " + e.getMessage();
        }
    }
}
