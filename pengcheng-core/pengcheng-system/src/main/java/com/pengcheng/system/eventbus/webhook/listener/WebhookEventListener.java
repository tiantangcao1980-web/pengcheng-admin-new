package com.pengcheng.system.eventbus.webhook.listener;

import com.pengcheng.system.eventbus.event.DomainEvent;
import com.pengcheng.system.eventbus.webhook.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 监听所有 DomainEvent，转换为 Webhook 投递。
 *
 * <p>异常隔离：listener 自身异常不影响事件发布方主流程（@Async + try-catch）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventListener {

    private final WebhookDeliveryService deliveryService;

    @Async
    @EventListener
    public void onDomainEvent(DomainEvent event) {
        try {
            deliveryService.enqueueForEvent(event);
        } catch (Exception e) {
            log.warn("[Webhook] enqueueForEvent 失败 eventCode={} eventId={}",
                    event.getEventCode(), event.getEventId(), e);
        }
    }
}
