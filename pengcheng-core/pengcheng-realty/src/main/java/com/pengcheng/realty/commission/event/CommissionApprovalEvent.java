package com.pengcheng.realty.commission.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 佣金审批领域事件
 *
 * 由 CommissionService 在每次审批节点流转时发布。
 * 订阅方（如消息通知模块、外部系统对接、AI 摘要）按需消费，避免硬依赖。
 */
@Getter
public class CommissionApprovalEvent extends ApplicationEvent {

    /** 佣金ID */
    private final Long commissionId;

    /** 流转前节点 */
    private final String fromNode;

    /** 流转后节点 */
    private final String toNode;

    /** 操作人ID */
    private final Long actorId;

    /** 操作类型：SUBMIT / APPROVE / REJECT / PAY */
    private final String action;

    /** 备注（驳回时通常必填） */
    private final String remark;

    public CommissionApprovalEvent(Object source, Long commissionId, String fromNode,
                                   String toNode, Long actorId, String action, String remark) {
        super(source);
        this.commissionId = commissionId;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.actorId = actorId;
        this.action = action;
        this.remark = remark;
    }

    /** 操作类型常量 */
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_PAY = "PAY";
}
