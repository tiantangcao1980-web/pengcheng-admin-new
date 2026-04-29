package com.pengcheng.ai.cost;

/**
 * 模型分级（V4.0 MVP 闭环④ AI 成本控制）。
 *
 * <ul>
 *     <li>SMALL  —— 兜底模型（如 qwen-turbo / hunyuan-lite）；廉价、快、用于短问答 &amp; 兜底</li>
 *     <li>MEDIUM —— 主力模型（如 qwen-plus / hunyuan-2.0-instruct）；常规对话/RAG</li>
 *     <li>LARGE  —— 复杂推理 / 长上下文（如 qwen-max / deepseek-v3.2）；按需开启</li>
 * </ul>
 */
public enum ModelTier {

    SMALL("qwen-turbo"),
    MEDIUM("qwen-plus"),
    LARGE("qwen-max");

    private final String defaultModelName;

    ModelTier(String defaultModelName) {
        this.defaultModelName = defaultModelName;
    }

    public String defaultModelName() {
        return defaultModelName;
    }
}
