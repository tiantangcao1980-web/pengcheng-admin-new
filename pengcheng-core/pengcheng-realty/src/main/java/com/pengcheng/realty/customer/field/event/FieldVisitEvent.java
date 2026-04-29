package com.pengcheng.realty.customer.field.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 销售外勤拜访领域事件
 *
 * 由 FieldVisitService 在签到/签退时发布。
 * 订阅方按需消费：
 *   - CustomerFollowupAutoListener: 签到自动写客户跟进时间线（主线挂在 customer 模块）
 *   - KPI 统计：每日外勤次数纳入业务员 KPI
 */
@Getter
public class FieldVisitEvent extends ApplicationEvent {

    private final Long fieldVisitId;
    private final Long userId;
    private final Long customerId;
    private final Long projectId;
    private final String action;  // CHECK_IN / CHECK_OUT
    private final Integer visitType;

    public FieldVisitEvent(Object source, Long fieldVisitId, Long userId,
                           Long customerId, Long projectId, String action,
                           Integer visitType) {
        super(source);
        this.fieldVisitId = fieldVisitId;
        this.userId = userId;
        this.customerId = customerId;
        this.projectId = projectId;
        this.action = action;
        this.visitType = visitType;
    }

    public static final String ACTION_CHECK_IN = "CHECK_IN";
    public static final String ACTION_CHECK_OUT = "CHECK_OUT";
}
