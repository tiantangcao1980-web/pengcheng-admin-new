package com.pengcheng.realty.receivable.listener;

import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.receivable.event.ReceivableOverdueAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 回款逾期告警通知订阅者
 *
 * 职责：把 ReceivableOverdueAlertEvent 转换成站内信发给对应业务员。
 * 解耦：ReceivableService 只发事件，本订阅者按需消费，不影响 Service 性能。
 *
 * 接收人查找链：dealId → CustomerDeal → customerId → Customer.creatorId（业务员）
 *
 * 异步执行避免阻塞 Quartz 任务线程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceivableAlertNotificationListener {

    private final CustomerDealMapper customerDealMapper;
    private final RealtyCustomerMapper customerMapper;
    private final NotificationService notificationService;

    /** 通知类型：复用现有 Notification.type 字段，约定 4 = 回款逾期告警 */
    private static final int NOTIFY_TYPE_RECEIVABLE_OVERDUE = 4;

    /** 业务类型常量 */
    private static final String BIZ_TYPE = "receivable_overdue";

    @Async
    @EventListener
    public void onOverdueAlert(ReceivableOverdueAlertEvent event) {
        try {
            // 1. 找接收人：dealId → CustomerDeal → customerId → Customer.creatorId
            Long receiverId = resolveReceiver(event.getDealId());
            if (receiverId == null) {
                log.warn("[回款告警通知] 找不到接收人，跳过 planId={} dealId={}",
                        event.getPlanId(), event.getDealId());
                return;
            }

            String customerName = resolveCustomerName(event.getDealId());

            // 2. 构造站内信
            String title = String.format("【回款逾期 · %s】%s",
                    event.getLevel().getLabel(),
                    customerName == null ? "未知客户" : customerName);
            String content = String.format(
                    "客户「%s」第 %d 期应收款逾期 %d 天。应付 %s 元，未收 %s 元，应付日期 %s。请尽快跟进。",
                    customerName == null ? "未知客户" : customerName,
                    event.getPlanId(),  // 暂以 planId 占位"第 N 期"，可在 V1.0 收尾时改为 periodNo
                    event.getDaysOverdue(),
                    event.getDueAmount() == null ? "-" : event.getDueAmount().toPlainString(),
                    event.getUnpaidAmount() == null ? "-" : event.getUnpaidAmount().toPlainString(),
                    event.getDueDate());

            Notification n = Notification.builder()
                    .userId(receiverId)
                    .title(title)
                    .content(content)
                    .type(NOTIFY_TYPE_RECEIVABLE_OVERDUE)
                    .bizType(BIZ_TYPE)
                    .bizId(event.getPlanId())
                    .readStatus(0)
                    .build();
            notificationService.createNotification(n);

            log.info("[回款告警通知] 已发送站内信 receiverId={} planId={} level={}",
                    receiverId, event.getPlanId(), event.getLevel());

            // TODO V1.0 收尾：调用 pengcheng-infra/pengcheng-wechat 发模板消息推送
            // wechatTemplateService.send(receiverId, RECEIVABLE_OVERDUE_TEMPLATE, params);

        } catch (Exception e) {
            // listener 不能抛异常，否则会影响事件发布方的事务（即使是 @Async 也避免破坏 publish 流程）
            log.error("[回款告警通知] 发送失败 planId={}: {}", event.getPlanId(), e.getMessage(), e);
        }
    }

    private Long resolveReceiver(Long dealId) {
        if (dealId == null) return null;
        CustomerDeal deal = customerDealMapper.selectById(dealId);
        if (deal == null || deal.getCustomerId() == null) return null;
        Customer customer = customerMapper.selectById(deal.getCustomerId());
        if (customer == null) return null;
        return customer.getCreatorId();
    }

    private String resolveCustomerName(Long dealId) {
        if (dealId == null) return null;
        CustomerDeal deal = customerDealMapper.selectById(dealId);
        if (deal == null || deal.getCustomerId() == null) return null;
        Customer customer = customerMapper.selectById(deal.getCustomerId());
        return customer == null ? null : customer.getCustomerName();
    }
}
