package com.pengcheng.ai.cost;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存实现：每天 0 点配额自动重置（按 LocalDate 切片）。
 * <p>
 * 仅用于单实例部署的 MVP 阶段；多实例需替换为 Redis HINCRBY + EXPIRE。
 */
@Component
@ConditionalOnMissingBean(value = AiQuotaStore.class, ignored = InMemoryAiQuotaStore.class)
public class InMemoryAiQuotaStore implements AiQuotaStore {

    /** 默认每个租户每日 100 万 token */
    public static final long DEFAULT_DAILY_QUOTA = 1_000_000L;

    private final Map<Long, AtomicLong> remaining = new ConcurrentHashMap<>();
    private final Map<Long, Long> dailyQuota = new ConcurrentHashMap<>();
    private final Map<Long, LocalDate> lastDay = new ConcurrentHashMap<>();

    @Override
    public synchronized long remainingTokens(Long tenantId) {
        Long key = normalize(tenantId);
        rolloverIfNewDay(key);
        return remaining.getOrDefault(key, new AtomicLong(getQuota(key))).get();
    }

    @Override
    public synchronized void consume(Long tenantId, long tokens) {
        if (tokens <= 0) {
            return;
        }
        Long key = normalize(tenantId);
        rolloverIfNewDay(key);
        AtomicLong v = remaining.computeIfAbsent(key, k -> new AtomicLong(getQuota(k)));
        v.addAndGet(-tokens);
    }

    @Override
    public synchronized void setDailyQuota(Long tenantId, long totalTokens) {
        Long key = normalize(tenantId);
        dailyQuota.put(key, totalTokens);
        remaining.put(key, new AtomicLong(totalTokens));
        lastDay.put(key, LocalDate.now());
    }

    private void rolloverIfNewDay(Long key) {
        LocalDate today = LocalDate.now();
        LocalDate seen = lastDay.get(key);
        if (seen == null || !seen.isEqual(today)) {
            lastDay.put(key, today);
            remaining.put(key, new AtomicLong(getQuota(key)));
        }
    }

    private long getQuota(Long key) {
        return dailyQuota.getOrDefault(key, DEFAULT_DAILY_QUOTA);
    }

    private Long normalize(Long tenantId) {
        return Objects.requireNonNullElse(tenantId, 0L);
    }
}
