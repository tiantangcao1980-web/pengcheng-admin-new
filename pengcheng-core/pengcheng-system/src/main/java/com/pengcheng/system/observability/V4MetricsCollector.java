package com.pengcheng.system.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * V4 MVP 核心业务指标收集器
 *
 * <p>暴露 4 类业务指标：
 * <ul>
 *   <li>pengcheng.openapi.requests   — OpenAPI 调用次数，按 ak 标签区分</li>
 *   <li>pengcheng.webhook.delivery   — Webhook 投递结果，按 status(SUCCESS/FAILED/DEAD) 区分</li>
 *   <li>pengcheng.ai.copilot.actions — AI Copilot 动作次数，按 action(FOLLOW/TODO/APPROVAL) 区分</li>
 *   <li>pengcheng.cards.render       — Dashboard 卡片渲染耗时，按 cardCode 区分</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class V4MetricsCollector {

    private final MeterRegistry registry;

    /** ak -> Counter 缓存，避免每次调用都 registry.counter() 查找 */
    private final ConcurrentMap<String, Counter> apiCounters = new ConcurrentHashMap<>();
    /** status -> Counter 缓存 */
    private final ConcurrentMap<String, Counter> webhookCounters = new ConcurrentHashMap<>();
    /** action -> Counter 缓存 */
    private final ConcurrentMap<String, Counter> copilotCounters = new ConcurrentHashMap<>();
    /** cardCode -> Timer 缓存 */
    private final ConcurrentMap<String, Timer> cardTimers = new ConcurrentHashMap<>();

    // ======================== OpenAPI ========================

    /**
     * 记录 OpenAPI 请求次数。
     *
     * @param ak Access Key，用于标识调用方
     */
    public void incrementApi(String ak) {
        apiCounters
            .computeIfAbsent(ak, key -> Counter.builder("pengcheng.openapi.requests")
                .description("OpenAPI 调用次数")
                .tag("ak", key)
                .register(registry))
            .increment();
    }

    // ======================== Webhook ========================

    /**
     * 记录 Webhook 投递结果。
     *
     * @param status 投递状态，枚举值：SUCCESS / FAILED / DEAD
     */
    public void recordWebhookDelivery(String status) {
        webhookCounters
            .computeIfAbsent(status, s -> Counter.builder("pengcheng.webhook.delivery")
                .description("Webhook 投递结果计数")
                .tag("status", s)
                .register(registry))
            .increment();
    }

    // ======================== AI Copilot ========================

    /**
     * 记录 AI Copilot 动作次数。
     *
     * @param action 动作类型，枚举值：FOLLOW / TODO / APPROVAL
     */
    public void recordCopilotAction(String action) {
        copilotCounters
            .computeIfAbsent(action, a -> Counter.builder("pengcheng.ai.copilot.actions")
                .description("AI Copilot 动作次数")
                .tag("action", a)
                .register(registry))
            .increment();
    }

    // ======================== Dashboard Cards ========================

    /**
     * 计时执行卡片渲染任务，并记录耗时。
     *
     * @param cardCode 卡片编码，如 CUSTOMER_POOL / FINANCE_COMMISSION
     * @param runnable 实际渲染逻辑
     */
    public void timeCardRender(String cardCode, Runnable runnable) {
        cardTimers
            .computeIfAbsent(cardCode, code -> Timer.builder("pengcheng.cards.render")
                .description("Dashboard 卡片渲染耗时")
                .tag("cardCode", code)
                .publishPercentileHistogram()
                .register(registry))
            .record(runnable);
    }
}
