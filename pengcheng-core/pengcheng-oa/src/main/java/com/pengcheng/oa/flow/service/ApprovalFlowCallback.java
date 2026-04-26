package com.pengcheng.oa.flow.service;

/**
 * 流程引擎在终态触发的回调，业务模块（如补卡）通过实现此接口感知。
 * <p>
 * 引擎按 bizType 路由到匹配的回调实现，可有 0~N 个实现，按 Spring Bean 全部派发。
 */
public interface ApprovalFlowCallback {

    /**
     * 该回调支持哪种 bizType（与 ApprovalInstance.bizType 对齐）。
     */
    String supportBizType();

    /**
     * 流程到达终态时调用。
     *
     * @param bizId    业务实体 ID
     * @param approved true=通过 false=驳回（含超时驳回）
     */
    void onComplete(Long bizId, boolean approved);
}
