package com.pengcheng.ai.provider;

import com.pengcheng.ai.config.AiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OllamaProvider 单测
 *
 * 不连真实 Ollama 服务，仅验证：
 *   - name() 返回 "ollama"
 *   - 不可达时 isAvailable() 返回 false 并缓存
 *   - generate() 输入为空时返回空串
 *   - 网络失败时返回降级文案而非抛异常
 */
@DisplayName("OllamaProvider — 本地推理")
class OllamaProviderTest {

    private OllamaProvider buildWithUnreachableUrl() {
        AiProperties props = new AiProperties();
        // 用一个保证不可达的端口避免依赖外部服务
        props.setOllamaBaseUrl("http://localhost:1");
        props.setOllamaModel("qwen2:7b");
        return new OllamaProvider(props);
    }

    @Test
    @DisplayName("name 返回 'ollama'")
    void nameOk() {
        assertThat(buildWithUnreachableUrl().name()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("不可达端点 isAvailable=false 且结果缓存")
    void isAvailable_unreachable() {
        OllamaProvider p = buildWithUnreachableUrl();
        assertThat(p.isAvailable()).isFalse();
        // 第二次调用走缓存
        assertThat(p.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("空输入 → 返回空串，不发起 HTTP")
    void generate_emptyInput() {
        OllamaProvider p = buildWithUnreachableUrl();
        assertThat(p.generate("sys", "")).isEmpty();
        assertThat(p.generate("sys", null)).isEmpty();
    }

    @Test
    @DisplayName("不可达端点 generate → 返回降级文案，不抛异常")
    void generate_failureGraceful() {
        OllamaProvider p = buildWithUnreachableUrl();
        String result = p.generate("sys", "你好");
        assertThat(result).contains("Ollama 调用失败");
    }
}
