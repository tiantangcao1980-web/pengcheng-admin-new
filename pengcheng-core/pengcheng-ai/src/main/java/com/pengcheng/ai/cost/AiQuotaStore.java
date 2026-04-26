package com.pengcheng.ai.cost;

/**
 * 企业配额存储（V4.0 MVP 闭环④）。
 *
 * <p>MVP 用内存实现 {@link InMemoryAiQuotaStore}（单实例够用）；后续可换 Redis 计数器。
 * 不依赖现有 RouterService / OrchestratorService（红线：只读引用）。
 */
public interface AiQuotaStore {

    /**
     * 当前 tenant 在 utc-day 内剩余可用 token 数（&lt;=0 表示已耗尽）。
     */
    long remainingTokens(Long tenantId);

    /**
     * 扣减 token（路由后估算扣减；真实 token 计费在响应回来后还可补扣）。
     */
    void consume(Long tenantId, long tokens);

    /**
     * 设置 / 重置 tenant 当日配额。
     */
    void setDailyQuota(Long tenantId, long totalTokens);
}
