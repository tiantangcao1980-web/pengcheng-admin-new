package com.pengcheng.ai.cost;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAiQuotaStoreTest {

    @Test
    void shouldReturnDefaultQuotaWhenNotSet() {
        InMemoryAiQuotaStore store = new InMemoryAiQuotaStore();
        assertThat(store.remainingTokens(1L)).isEqualTo(InMemoryAiQuotaStore.DEFAULT_DAILY_QUOTA);
    }

    @Test
    void shouldDeductOnConsume() {
        InMemoryAiQuotaStore store = new InMemoryAiQuotaStore();
        store.setDailyQuota(1L, 1000);
        store.consume(1L, 300);
        assertThat(store.remainingTokens(1L)).isEqualTo(700);
    }

    @Test
    void shouldGoNegativeWhenOverConsumed() {
        InMemoryAiQuotaStore store = new InMemoryAiQuotaStore();
        store.setDailyQuota(2L, 100);
        store.consume(2L, 250);
        assertThat(store.remainingTokens(2L)).isLessThanOrEqualTo(0);
    }

    @Test
    void shouldHandleNullTenantAsZero() {
        InMemoryAiQuotaStore store = new InMemoryAiQuotaStore();
        store.consume(null, 50);
        // 不抛异常即可
        assertThat(store.remainingTokens(null))
                .isLessThan(InMemoryAiQuotaStore.DEFAULT_DAILY_QUOTA);
    }

    @Test
    void shouldIgnoreNonPositiveConsumption() {
        InMemoryAiQuotaStore store = new InMemoryAiQuotaStore();
        store.setDailyQuota(3L, 1000);
        store.consume(3L, 0);
        store.consume(3L, -10);
        assertThat(store.remainingTokens(3L)).isEqualTo(1000);
    }
}
