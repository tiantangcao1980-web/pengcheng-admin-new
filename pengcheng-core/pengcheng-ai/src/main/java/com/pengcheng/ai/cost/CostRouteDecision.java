package com.pengcheng.ai.cost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostRouteDecision {

    /** 选中的模型档次 */
    private ModelTier tier;

    /** 选中的具体模型名（默认按 ModelTier#defaultModelName） */
    private String modelName;

    /** 决策理由（用于审计/日志） */
    private String reason;

    /** 是否因为配额耗尽被强制降级 */
    private boolean quotaExceeded;
}
