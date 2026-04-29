package com.pengcheng.ai.copilot.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ConfirmTokenStore} — InMemory 实现单测。
 *
 * <p>覆盖 6 个场景：
 * <ol>
 *   <li>issue 返回 32 字符 token（URL-safe Base64，无 '='）</li>
 *   <li>verifyAndConsume 成功后 token 即作废（一次性消费）</li>
 *   <li>TTL 过期后 verifyAndConsume 返回 empty</li>
 *   <li>userId 不匹配时返回 empty（防越权）</li>
 *   <li>不存在的 token 返回 empty</li>
 *   <li>并发场景：N 个线程同时 consume，只有 1 个成功</li>
 * </ol>
 */
class ConfirmTokenStoreTest {

    private InMemoryConfirmTokenStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryConfirmTokenStore();
    }

    // ------------------------------------------------------------------
    // 用例 1：issue 返回 32 字符 nanoid 风格 token
    // ------------------------------------------------------------------
    @Test
    void issue_shouldReturnThirtyTwoCharToken() {
        String token = store.issue(proposal(1L), 1L);

        assertThat(token)
                .isNotBlank()
                .hasSize(32)
                // URL-safe Base64 无填充：只含 A-Z a-z 0-9 - _
                .matches("[A-Za-z0-9_\\-]{32}");
    }

    // ------------------------------------------------------------------
    // 用例 2：一次消费即作废
    // ------------------------------------------------------------------
    @Test
    void verifyAndConsume_shouldBeOneShot() {
        String token = store.issue(proposal(1L), 1L);

        Optional<CopilotActionProposal> first = store.verifyAndConsume(token, 1L);
        Optional<CopilotActionProposal> second = store.verifyAndConsume(token, 1L);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
    }

    // ------------------------------------------------------------------
    // 用例 3：TTL 过期返回 empty
    // ------------------------------------------------------------------
    @Test
    void verifyAndConsume_shouldRejectExpiredToken() throws Exception {
        String token = store.issue(proposal(2L), 2L);

        // 强行把缓存中该条目的 expireAt 改到过去
        forceExpire(token);

        Optional<CopilotActionProposal> result = store.verifyAndConsume(token, 2L);
        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // 用例 4：userId 不匹配拒绝
    // ------------------------------------------------------------------
    @Test
    void verifyAndConsume_shouldRejectMismatchedUserId() {
        String token = store.issue(proposal(3L), 3L);

        Optional<CopilotActionProposal> result = store.verifyAndConsume(token, 99L);
        assertThat(result).isEmpty();

        // 原始 userId 仍可消费（条目未被删除）
        Optional<CopilotActionProposal> legit = store.verifyAndConsume(token, 3L);
        assertThat(legit).isPresent();
    }

    // ------------------------------------------------------------------
    // 用例 5：不存在的 token 返回 empty
    // ------------------------------------------------------------------
    @Test
    void verifyAndConsume_shouldReturnEmptyForUnknownToken() {
        Optional<CopilotActionProposal> result = store.verifyAndConsume("no-such-token", 1L);
        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // 用例 6：并发安全 —— N 个线程同时 consume，只有 1 个成功
    // ------------------------------------------------------------------
    @Test
    void verifyAndConsume_shouldBeThreadSafe() throws InterruptedException {
        String token = store.issue(proposal(4L), 4L);

        int threads = 16;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                Optional<CopilotActionProposal> r = store.verifyAndConsume(token, 4L);
                if (r.isPresent()) successCount.incrementAndGet();
            });
            workers.add(t);
            t.start();
        }

        ready.await();
        start.countDown();
        for (Thread t : workers) t.join(2000);

        // 只有 1 个线程成功消费
        assertThat(successCount.get()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------

    private static CopilotActionProposal proposal(Long actionId) {
        return new CopilotActionProposal(actionId, "token-placeholder", "PENDING", "新建客户跟进（客户：王总）");
    }

    /**
     * 通过反射把 InMemoryConfirmTokenStore 内部 cache 中指定 token 的
     * expireAt 改成 1 秒前，模拟 TTL 已过期。
     */
    @SuppressWarnings("unchecked")
    private void forceExpire(String token) throws Exception {
        Field cacheField = InMemoryConfirmTokenStore.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        ConcurrentHashMap<String, Object> cache =
                (ConcurrentHashMap<String, Object>) cacheField.get(store);

        Object entry = cache.get(token);
        if (entry == null) return;

        Field expireAtField = entry.getClass().getDeclaredField("expireAt");
        expireAtField.setAccessible(true);
        expireAtField.set(entry, Instant.now().minusSeconds(10));
    }
}
