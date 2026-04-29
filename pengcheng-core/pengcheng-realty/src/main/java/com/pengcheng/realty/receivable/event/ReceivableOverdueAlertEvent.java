package com.pengcheng.realty.receivable.event;

import com.pengcheng.realty.receivable.enums.OverdueAlertLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回款逾期告警领域事件
 *
 * 由 ReceivableService 在每次"档位升级"时发布。订阅方（站内信、微信推送、邮件）按需消费。
 * 同档位不会重复发布事件，避免通知风暴。
 */
@Getter
public class ReceivableOverdueAlertEvent extends ApplicationEvent {

    private final Long planId;
    private final Long dealId;
    private final OverdueAlertLevel level;
    private final int daysOverdue;
    private final BigDecimal dueAmount;
    private final BigDecimal unpaidAmount;
    private final LocalDate dueDate;

    public ReceivableOverdueAlertEvent(Object source, Long planId, Long dealId,
                                       OverdueAlertLevel level, int daysOverdue,
                                       BigDecimal dueAmount, BigDecimal unpaidAmount,
                                       LocalDate dueDate) {
        super(source);
        this.planId = planId;
        this.dealId = dealId;
        this.level = level;
        this.daysOverdue = daysOverdue;
        this.dueAmount = dueAmount;
        this.unpaidAmount = unpaidAmount;
        this.dueDate = dueDate;
    }
}
