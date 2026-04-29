package com.pengcheng.ai.rag;

import lombok.Getter;

/**
 * 房产销售知识库分桶（V1.0 Sprint B 第 6 任务）
 *
 * 复用 KnowledgeBaseService 已有的 projectId 隔离机制，
 * 用约定的负数 projectId 作为系统级"全局桶"标识，避免与真实项目 ID 冲突。
 *
 *   PROJECT     ：楼盘库（户型/配套/价格/楼书）—— projectId = -1001
 *   SCRIPT      ：销售话术库（异议处理/跟进话术/谈判技巧）—— projectId = -1002
 *   POLICY      ：政策法规库（限购/贷款/税费/公积金）—— projectId = -1003
 */
@Getter
public enum RealtyKnowledgeBucket {

    PROJECT("楼盘库",    -1001L),
    SCRIPT("销售话术库",  -1002L),
    POLICY("政策法规库",  -1003L);

    private final String displayName;
    private final Long projectId;

    RealtyKnowledgeBucket(String displayName, Long projectId) {
        this.displayName = displayName;
        this.projectId = projectId;
    }

    public static RealtyKnowledgeBucket fromCode(String code) {
        for (RealtyKnowledgeBucket b : values()) {
            if (b.name().equalsIgnoreCase(code)) return b;
        }
        throw new IllegalArgumentException("未知知识库桶: " + code);
    }
}
