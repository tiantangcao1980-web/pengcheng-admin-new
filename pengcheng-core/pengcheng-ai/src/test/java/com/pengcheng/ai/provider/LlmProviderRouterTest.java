package com.pengcheng.ai.provider;

import com.pengcheng.ai.config.AiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LlmProviderRouter 选择策略单测
 */
@DisplayName("LlmProviderRouter — Provider 选择")
class LlmProviderRouterTest {

    /** 测试用 Provider 桩 */
    static class StubProvider implements LlmProvider {
        private final String name;
        private final boolean available;
        private final String response;

        StubProvider(String name, boolean available, String response) {
            this.name = name;
            this.available = available;
            this.response = response;
        }

        @Override public String name() { return name; }
        @Override public boolean isAvailable() { return available; }
        @Override public String generate(String s, String u) { return response; }
    }

    private AiProperties propsWith(String provider) {
        AiProperties p = new AiProperties();
        p.setProvider(provider);
        return p;
    }

    @Test
    @DisplayName("auto 模式：返回第一个可用 Provider")
    void auto_returnsFirstAvailable() {
        StubProvider dashscope = new StubProvider("dashscope", true, "ds-resp");
        StubProvider ollama = new StubProvider("ollama", true, "ol-resp");

        LlmProviderRouter router = new LlmProviderRouter(
                List.of(dashscope, ollama), propsWith("auto"));

        assertThat(router.resolve().name()).isEqualTo("dashscope");
        assertThat(router.generate("sys", "hi")).isEqualTo("ds-resp");
    }

    @Test
    @DisplayName("auto 模式：第一个不可用时跳过，取下一个可用")
    void auto_skipsUnavailable() {
        StubProvider dashscope = new StubProvider("dashscope", false, "ds-resp");
        StubProvider ollama = new StubProvider("ollama", true, "ol-resp");

        LlmProviderRouter router = new LlmProviderRouter(
                List.of(dashscope, ollama), propsWith("auto"));

        assertThat(router.resolve().name()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("配置指定 ollama 时优先选 ollama")
    void configured_picksOllama() {
        StubProvider dashscope = new StubProvider("dashscope", true, "ds-resp");
        StubProvider ollama = new StubProvider("ollama", true, "ol-resp");

        LlmProviderRouter router = new LlmProviderRouter(
                List.of(dashscope, ollama), propsWith("ollama"));

        assertThat(router.resolve().name()).isEqualTo("ollama");
        assertThat(router.generate("sys", "hi")).isEqualTo("ol-resp");
    }

    @Test
    @DisplayName("配置指定不可用时回退到第一个可用")
    void configured_fallsBackWhenUnavailable() {
        StubProvider dashscope = new StubProvider("dashscope", true, "ds-resp");
        StubProvider ollama = new StubProvider("ollama", false, "ol-resp");

        LlmProviderRouter router = new LlmProviderRouter(
                List.of(dashscope, ollama), propsWith("ollama"));

        assertThat(router.resolve().name()).isEqualTo("dashscope");
    }

    @Test
    @DisplayName("显式指定 prefer 优先于配置")
    void explicit_overridesConfig() {
        StubProvider dashscope = new StubProvider("dashscope", true, "ds-resp");
        StubProvider ollama = new StubProvider("ollama", true, "ol-resp");

        LlmProviderRouter router = new LlmProviderRouter(
                List.of(dashscope, ollama), propsWith("dashscope"));

        // 配置 dashscope，但显式 prefer ollama → 走 ollama
        assertThat(router.resolve("ollama").name()).isEqualTo("ollama");
        assertThat(router.generate("ollama", "sys", "hi")).isEqualTo("ol-resp");
    }

    @Test
    @DisplayName("全部不可用：返回首个供调用方降级")
    void allUnavailable_returnsFirst() {
        StubProvider dashscope = new StubProvider("dashscope", false, "ds-resp");
        StubProvider ollama = new StubProvider("ollama", false, "ol-resp");

        LlmProviderRouter router = new LlmProviderRouter(
                List.of(dashscope, ollama), propsWith("auto"));

        assertThat(router.resolve().name()).isEqualTo("dashscope");
    }

    @Test
    @DisplayName("无 Provider 注册：generate 返回路由失败提示")
    void noProvider_returnsFailureMessage() {
        LlmProviderRouter router = new LlmProviderRouter(
                List.of(), propsWith("auto"));

        assertThat(router.resolve()).isNull();
        assertThat(router.generate("sys", "hi")).contains("路由失败");
    }
}
