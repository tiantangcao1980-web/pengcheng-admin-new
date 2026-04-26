package com.pengcheng.ai.cost;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V4.0 D4 闭环④ AI 成本分级路由单测。
 *
 * <p>覆盖所有 6 条规则路径 + 配额扣减。
 */
class CostRouterServiceTest {

    private InMemoryAiQuotaStore quotaStore;
    private CostRouterService router;

    @BeforeEach
    void setUp() {
        quotaStore = new InMemoryAiQuotaStore();
        router = new CostRouterService(quotaStore);
        ReflectionTestUtils.setField(router, "largeTokenThreshold", 4000);
        ReflectionTestUtils.setField(router, "mediumTokenThreshold", 1000);
        ReflectionTestUtils.setField(router, "mediumHistoryTurns", 5);
        ReflectionTestUtils.setField(router, "consumeMultiplier", 1.0);
    }

    @Test
    void smallTier_shortQuery() {
        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(1L).tokensEstimate(200).historyTurns(1).build());
        assertThat(d.getTier()).isEqualTo(ModelTier.SMALL);
        assertThat(d.isQuotaExceeded()).isFalse();
    }

    @Test
    void mediumTier_byTokens() {
        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(1L).tokensEstimate(1500).historyTurns(1).build());
        assertThat(d.getTier()).isEqualTo(ModelTier.MEDIUM);
        assertThat(d.getReason()).contains("MEDIUM");
    }

    @Test
    void mediumTier_byHistoryTurns() {
        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(1L).tokensEstimate(300).historyTurns(6).build());
        assertThat(d.getTier()).isEqualTo(ModelTier.MEDIUM);
    }

    @Test
    void largeTier_byTokens() {
        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(1L).tokensEstimate(5000).historyTurns(1).build());
        assertThat(d.getTier()).isEqualTo(ModelTier.LARGE);
    }

    @Test
    void largeTier_byMultiModal() {
        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(1L).tokensEstimate(50).multiModal(true).build());
        assertThat(d.getTier()).isEqualTo(ModelTier.LARGE);
        assertThat(d.getReason()).contains("multi-modal");
    }

    @Test
    void largeTier_byHighQualityFlag() {
        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(1L).tokensEstimate(100).preferHighQuality(true).build());
        assertThat(d.getTier()).isEqualTo(ModelTier.LARGE);
    }

    @Test
    void shouldEscalateAcrossThresholds_smallToMediumToLarge() {
        // small
        assertThat(router.decide(CostRouteRequest.builder()
                .tenantId(2L).tokensEstimate(500).build()).getTier())
                .isEqualTo(ModelTier.SMALL);
        // medium
        assertThat(router.decide(CostRouteRequest.builder()
                .tenantId(2L).tokensEstimate(1200).build()).getTier())
                .isEqualTo(ModelTier.MEDIUM);
        // large
        assertThat(router.decide(CostRouteRequest.builder()
                .tenantId(2L).tokensEstimate(8000).build()).getTier())
                .isEqualTo(ModelTier.LARGE);
    }

    @Test
    void quotaExceeded_shouldForceSmall() {
        quotaStore.setDailyQuota(7L, 100);
        // 消耗超过配额
        router.consumePreCall(7L, 200);

        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(7L).tokensEstimate(8000).preferHighQuality(true).build());
        assertThat(d.getTier()).isEqualTo(ModelTier.SMALL);
        assertThat(d.isQuotaExceeded()).isTrue();
        assertThat(d.getReason()).contains("quota exceeded");
    }

    @Test
    void modelName_shouldFallbackToTierDefault() {
        CostRouteDecision d = router.decide(CostRouteRequest.builder()
                .tenantId(1L).tokensEstimate(1500).build());
        assertThat(d.getModelName()).isEqualTo(ModelTier.MEDIUM.defaultModelName());
    }
}
