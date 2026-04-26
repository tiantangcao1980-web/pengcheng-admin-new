package com.pengcheng.ai.cost;

import lombok.Builder;
import lombok.Data;

/**
 * 模型分级路由判断输入。
 *
 * <p>命中策略（按优先级，第一条满足即返回）：
 * <ol>
 *     <li>tenant 当日配额已耗尽 → 强制 SMALL（兜底）；</li>
 *     <li>需要图像/多模态 → LARGE；</li>
 *     <li>tokensEstimate &gt;= largeThreshold（默认 4000） → LARGE；</li>
 *     <li>tokensEstimate &gt;= mediumThreshold（默认 1000） 或 多轮上下文 &gt; 4 → MEDIUM；</li>
 *     <li>其它 → SMALL。</li>
 * </ol>
 */
@Data
@Builder
public class CostRouteRequest {

    /** 租户 ID（用于配额隔离，可空） */
    private Long tenantId;

    /** 估算的 prompt token 数 */
    private int tokensEstimate;

    /** 多轮对话上下文消息条数 */
    private int historyTurns;

    /** 是否多模态（图像/音频） */
    private boolean multiModal;

    /** 是否要求高质量推理（用户显式选择"高质量"模式） */
    private boolean preferHighQuality;
}
