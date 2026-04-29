package com.pengcheng.ai.provider;

/**
 * LLM 大模型 Provider 抽象
 *
 * 由 LlmProviderRouter 统一管理，按配置或动态策略选择具体实现：
 *   - DashScopeProvider：阿里通义（默认，复用现有 ChatClient）
 *   - OllamaProvider   ：本地 Ollama（私有化部署，复用 HTTP API）
 *   - 未来扩展：DeepSeekProvider / OpenAIProvider / ZhipuProvider 等
 *
 * 接口最小化：核心只暴露 generate(systemPrompt, userInput)；
 * 流式 / Function Calling 暂保留在 ChatClient 层（V2.0 再统一抽象）。
 */
public interface LlmProvider {

    /** Provider 名称（用于配置匹配，如 "dashscope" / "ollama"） */
    String name();

    /** 是否启用（缺失 API key / 不可达时返回 false） */
    boolean isAvailable();

    /**
     * 同步生成（系统提示 + 用户输入 → 文本回复）
     *
     * @param systemPrompt 系统提示词，可为 null
     * @param userInput 用户输入
     * @return 模型回复文本；调用失败时由实现自行抛 RuntimeException 或返回 fallback
     */
    String generate(String systemPrompt, String userInput);
}
