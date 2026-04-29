package com.pengcheng.realty.commission.enums;

import lombok.Getter;

/**
 * 佣金审批节点
 *
 * 流转：DRAFT → SUBMITTED → MANAGER_APPROVED → FINANCE_APPROVED → PAID
 * 任意节点可 → REJECTED（驳回回到 DRAFT 或保留 REJECTED 终态由业务决定）
 */
@Getter
public enum CommissionApprovalNode {

    DRAFT("草稿", 0),
    SUBMITTED("已提交（待主管审批）", 1),
    MANAGER_APPROVED("主管已审（待财务审批）", 2),
    FINANCE_APPROVED("财务已审（待放款）", 3),
    PAID("已放款", 4),
    REJECTED("已驳回", -1);

    private final String label;
    private final int order;

    CommissionApprovalNode(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public boolean isTerminal() {
        return this == PAID || this == REJECTED;
    }
}
