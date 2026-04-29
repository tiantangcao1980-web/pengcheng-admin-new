package com.pengcheng.system.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V4MetricsCollector 单元测试
 *
 * <p>使用内存 SimpleMeterRegistry，无需 Spring 上下文。
 */
@DisplayName("V4MetricsCollector 指标采集测试")
class V4MetricsCollectorTest {

    private MeterRegistry registry;
    private V4MetricsCollector collector;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        collector = new V4MetricsCollector(registry);
    }

    // ======================== 用例 1：incrementApi ========================

    @Test
    @DisplayName("incrementApi — 同一 AK 调用 3 次，Counter 累计为 3")
    void incrementApi_shouldAccumulateCountByAk() {
        String ak = "ak-test-001";

        collector.incrementApi(ak);
        collector.incrementApi(ak);
        collector.incrementApi(ak);

        Counter counter = registry.find("pengcheng.openapi.requests")
                .tag("ak", ak)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    // ======================== 用例 2：recordWebhookDelivery ========================

    @Test
    @DisplayName("recordWebhookDelivery — 不同 status 各自独立累加")
    void recordWebhookDelivery_shouldCountByStatus() {
        collector.recordWebhookDelivery("SUCCESS");
        collector.recordWebhookDelivery("SUCCESS");
        collector.recordWebhookDelivery("FAILED");
        collector.recordWebhookDelivery("DEAD");
        collector.recordWebhookDelivery("DEAD");
        collector.recordWebhookDelivery("DEAD");

        assertThat(registry.find("pengcheng.webhook.delivery").tag("status", "SUCCESS").counter())
                .isNotNull()
                .extracting(Counter::count).isEqualTo(2.0);

        assertThat(registry.find("pengcheng.webhook.delivery").tag("status", "FAILED").counter())
                .isNotNull()
                .extracting(Counter::count).isEqualTo(1.0);

        assertThat(registry.find("pengcheng.webhook.delivery").tag("status", "DEAD").counter())
                .isNotNull()
                .extracting(Counter::count).isEqualTo(3.0);
    }

    // ======================== 用例 3：recordCopilotAction ========================

    @Test
    @DisplayName("recordCopilotAction — 多种 action label 独立计数")
    void recordCopilotAction_shouldCountByAction() {
        collector.recordCopilotAction("FOLLOW");
        collector.recordCopilotAction("FOLLOW");
        collector.recordCopilotAction("TODO");
        collector.recordCopilotAction("APPROVAL");

        assertThat(registry.find("pengcheng.ai.copilot.actions").tag("action", "FOLLOW").counter())
                .isNotNull()
                .extracting(Counter::count).isEqualTo(2.0);

        assertThat(registry.find("pengcheng.ai.copilot.actions").tag("action", "TODO").counter())
                .isNotNull()
                .extracting(Counter::count).isEqualTo(1.0);

        assertThat(registry.find("pengcheng.ai.copilot.actions").tag("action", "APPROVAL").counter())
                .isNotNull()
                .extracting(Counter::count).isEqualTo(1.0);
    }

    // ======================== 用例 4：timeCardRender ========================

    @Test
    @DisplayName("timeCardRender — Runnable 被执行且 Timer 有记录")
    void timeCardRender_shouldRecordTimingAndExecuteRunnable() {
        AtomicBoolean executed = new AtomicBoolean(false);
        String cardCode = "CUSTOMER_POOL";

        collector.timeCardRender(cardCode, () -> {
            executed.set(true);
            // 模拟简单耗时操作（不引入 Thread.sleep）
            long sum = 0;
            for (int i = 0; i < 10_000; i++) {
                sum += i;
            }
        });

        // Runnable 应被执行
        assertThat(executed).isTrue();

        // Timer 应存在且记录了 1 次
        Timer timer = registry.find("pengcheng.cards.render")
                .tag("cardCode", cardCode)
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS)).isGreaterThan(0);
    }
}
