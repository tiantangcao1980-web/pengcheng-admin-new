package com.pengcheng.ai.cost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 成本分级路由（V4.0 MVP 闭环④）。
 *
 * <p>约束：本服务仅做 <b>分级建议</b>，<b>不修改</b> {@code RouterService} /
 * {@code OrchestratorService}（红线：只读引用），由调用侧（如 AiChatController 上层）
 * 在调用前查询本服务，再按结果选择具体 ChatModel 实例。
 *
 * <pre>
 * 触发顺序（first-match-wins）：
 *   1. 配额耗尽            → SMALL（兜底）
 *   2. 多模态请求          → LARGE
 *   3. 显式偏好高质量      → LARGE
 *   4. tokens >= largeT    → LARGE
 *   5. tokens >= mediumT 或 historyTurns >= 5 → MEDIUM
 *   6. else                → SMALL
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostRouterService {

    private final AiQuotaStore quotaStore;

    /** tokens >= 该阈值 → LARGE（默认 4000） */
    @Value("${pengcheng.ai.cost.large-token-threshold:4000}")
    private int largeTokenThreshold;

    /** tokens >= 该阈值 → MEDIUM（默认 1000） */
    @Value("${pengcheng.ai.cost.medium-token-threshold:1000}")
    private int mediumTokenThreshold;

    /** historyTurns >= 该值 → MEDIUM（默认 5） */
    @Value("${pengcheng.ai.cost.medium-history-turns:5}")
    private int mediumHistoryTurns;

    /** 单次 LLM 调用估算 token 上限（用于扣减时不至于失真） */
    @Value("${pengcheng.ai.cost.consume-multiplier:1.2}")
    private double consumeMultiplier;

    /**
     * 决策模型档次。
     */
    public CostRouteDecision decide(CostRouteRequest req) {
        long remaining = quotaStore.remainingTokens(req.getTenantId());
        if (remaining <= 0) {
            return new CostRouteDecision(ModelTier.SMALL, ModelTier.SMALL.defaultModelName(),
                    "quota exceeded → forced SMALL", true);
        }
        if (req.isMultiModal()) {
            return new CostRouteDecision(ModelTier.LARGE, ModelTier.LARGE.defaultModelName(),
                    "multi-modal → LARGE", false);
        }
        if (req.isPreferHighQuality()) {
            return new CostRouteDecision(ModelTier.LARGE, ModelTier.LARGE.defaultModelName(),
                    "user prefers high quality → LARGE", false);
        }
        if (req.getTokensEstimate() >= largeTokenThreshold) {
            return new CostRouteDecision(ModelTier.LARGE, ModelTier.LARGE.defaultModelName(),
                    "tokens(" + req.getTokensEstimate() + ") >= " + largeTokenThreshold + " → LARGE", false);
        }
        if (req.getTokensEstimate() >= mediumTokenThreshold || req.getHistoryTurns() >= mediumHistoryTurns) {
            return new CostRouteDecision(ModelTier.MEDIUM, ModelTier.MEDIUM.defaultModelName(),
                    "tokens=" + req.getTokensEstimate() + " history=" + req.getHistoryTurns() + " → MEDIUM", false);
        }
        return new CostRouteDecision(ModelTier.SMALL, ModelTier.SMALL.defaultModelName(),
                "default → SMALL", false);
    }

    /**
     * 在调用 LLM 前预扣减配额（按估算 token * consumeMultiplier）。
     */
    public void consumePreCall(Long tenantId, int tokensEstimate) {
        long consume = Math.max(1L, Math.round(tokensEstimate * consumeMultiplier));
        quotaStore.consume(tenantId, consume);
    }
}
