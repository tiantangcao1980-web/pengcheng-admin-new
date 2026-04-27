package com.pengcheng.system.eventbus.webhook.service;

import com.pengcheng.system.eventbus.event.DomainEvent;

/**
 * Webhook 投递服务。
 *
 * <p>核心契约：
 * <ul>
 *   <li>{@link #enqueueForEvent} 由 {@code WebhookEventListener} 调用：找匹配的
 *       订阅 → 创建 PENDING 记录；</li>
 *   <li>{@link #attemptDeliver} 实际 POST：
 *       <ul>
 *         <li>2xx → status=SUCCESS</li>
 *         <li>非 2xx 或网络异常 → attempt_count++，按指数退避算 next_attempt_at；
 *             attempt_count ≥ 5 时 status=DEAD</li>
 *       </ul></li>
 *   <li>{@link #replayDead} 手动重试已死信的投递（attempt_count=0 重新入队）。</li>
 * </ul>
 */
public interface WebhookDeliveryService {

    void enqueueForEvent(DomainEvent event);

    /** @return 投递成功返回 true */
    boolean attemptDeliver(Long deliveryId);

    void replayDead(Long deliveryId);

    /** 退避序列：1m → 5m → 30m → 2h → 12h → DEAD */
    static long backoffSecondsByAttempt(int attemptCount) {
        switch (attemptCount) {
            case 0: return 60;
            case 1: return 300;
            case 2: return 1800;
            case 3: return 7200;
            case 4: return 43200;
            default: return 0; // ≥5 由调用方标记 DEAD
        }
    }
}
