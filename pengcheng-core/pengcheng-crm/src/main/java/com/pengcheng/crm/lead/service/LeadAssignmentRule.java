package com.pengcheng.crm.lead.service;

import java.util.List;
import java.util.Map;

/**
 * 线索分配规则引擎（纯函数，可单测）
 * <p>支持 4 种 ruleType：
 * <ul>
 *   <li>manual：直接返回 targetUserId</li>
 *   <li>round_robin：按候选池下标轮询；状态由 currentLoad 中"轮询游标"键持久化</li>
 *   <li>load_balance：选 currentLoad 中数值最小的候选人（负载均衡）</li>
 *   <li>rule：按 mappingRules 中的 source -> userId 映射，命中即返回；未命中回退 round_robin</li>
 * </ul>
 */
public final class LeadAssignmentRule {

    public static final String ROUND_ROBIN_CURSOR = "__rr_cursor__";

    private LeadAssignmentRule() {}

    /**
     * @param ruleType        分配规则
     * @param targetUserId    manual 模式的目标
     * @param candidates      候选人列表
     * @param currentLoad     候选人当前负载（user_id -> 当前线索数）；可包含 ROUND_ROBIN_CURSOR 游标
     * @param leadSource      当前线索 source（rule 模式用）
     * @param sourceMapping   source -> userId
     * @return 选中的 userId；非法输入返回 null
     */
    public static Long pick(String ruleType,
                            Long targetUserId,
                            List<Long> candidates,
                            Map<Long, Integer> currentLoad,
                            String leadSource,
                            Map<String, Long> sourceMapping) {
        if (ruleType == null) {
            ruleType = "manual";
        }
        switch (ruleType) {
            case "manual":
                return targetUserId;

            case "round_robin": {
                if (candidates == null || candidates.isEmpty()) return null;
                int cursor = 0;
                if (currentLoad != null && currentLoad.containsKey((long) ROUND_ROBIN_CURSOR.hashCode())) {
                    cursor = currentLoad.get((long) ROUND_ROBIN_CURSOR.hashCode());
                }
                Long picked = candidates.get(Math.floorMod(cursor, candidates.size()));
                if (currentLoad != null) {
                    currentLoad.put((long) ROUND_ROBIN_CURSOR.hashCode(), cursor + 1);
                }
                return picked;
            }

            case "load_balance": {
                if (candidates == null || candidates.isEmpty()) return null;
                Long best = null;
                int bestLoad = Integer.MAX_VALUE;
                for (Long uid : candidates) {
                    int load = currentLoad == null ? 0 : currentLoad.getOrDefault(uid, 0);
                    if (load < bestLoad) {
                        bestLoad = load;
                        best = uid;
                    }
                }
                return best;
            }

            case "rule": {
                if (sourceMapping != null && leadSource != null) {
                    Long mapped = sourceMapping.get(leadSource);
                    if (mapped != null) return mapped;
                }
                // 未命中规则，回退轮询
                return pick("round_robin", null, candidates, currentLoad, leadSource, null);
            }

            default:
                return null;
        }
    }
}
