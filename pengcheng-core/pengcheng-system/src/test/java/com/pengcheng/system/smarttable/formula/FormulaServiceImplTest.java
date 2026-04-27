package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.formula.FormulaAst.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * FormulaServiceImpl 单元测试 — 4 用例
 *
 * 覆盖：compile 缓存命中、并发 compile 安全、
 *       evaluate 异常返回 #ERROR!、缓存上限驱逐
 */
@DisplayName("FormulaServiceImpl — Service 层测试")
class FormulaServiceImplTest {

    private FormulaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FormulaServiceImpl();
    }

    // ========================= TC-S01: compile 缓存命中 =========================

    @Test
    @DisplayName("TC-S01: 相同表达式 compile 应返回同一 AST 对象（缓存命中）")
    void tc01_compileCacheHit() {
        String expr = "{amount} * 1.1 + 5";
        Node first  = service.compile(expr);
        Node second = service.compile(expr);
        // 同一实例 → 缓存命中
        assertThat(first).isSameAs(second);
    }

    // ========================= TC-S02: 并发 compile 安全 =========================

    @Test
    @DisplayName("TC-S02: 并发 compile 同一表达式不应出现竞争或重复解析错误")
    void tc02_concurrentCompileSafe() throws InterruptedException {
        String expr = "ROUND({price}, 2) + IF({qty} > 0, {discount}, 0)";
        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    Node node = service.compile(expr);
                    assertThat(node).isNotNull();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();                           // 齐头并进
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        assertThat(errors.get()).isZero();
    }

    // ========================= TC-S03: evaluate 异常返回 #ERROR! =========================

    @Test
    @DisplayName("TC-S03: evaluate 遇到除以零等运行时错误应返回 #ERROR! 而不是抛出异常")
    void tc03_evaluateExceptionReturnsError() {
        // 除以零
        Object result1 = service.evaluate("{a} / {b}",
                java.util.Map.of("a", 10, "b", 0), Collections.emptyList());
        assertThat(result1).isEqualTo("#ERROR!");

        // 语法错误公式（compile 失败）
        Object result2 = service.evaluate("1 + + 2", Collections.emptyMap(), Collections.emptyList());
        assertThat(result2).isEqualTo("#ERROR!");
    }

    // ========================= TC-S04: 缓存上限驱逐 =========================

    @Test
    @DisplayName("TC-S04: 缓存超过 MAX_CACHE_SIZE 时触发驱逐，不抛出异常")
    void tc04_cacheEviction() {
        int limit = FormulaServiceImpl.MAX_CACHE_SIZE;
        // 写入 limit + 10 个不同表达式
        for (int i = 0; i < limit + 10; i++) {
            String expr = String.valueOf(i);   // "0", "1", ...  每个都是不同的数字字面量
            assertThatCode(() -> service.compile(expr)).doesNotThrowAnyException();
        }
        // 服务依然可用（没有因驱逐崩溃）
        assertThatCode(() -> service.compile("999999")).doesNotThrowAnyException();
    }
}
