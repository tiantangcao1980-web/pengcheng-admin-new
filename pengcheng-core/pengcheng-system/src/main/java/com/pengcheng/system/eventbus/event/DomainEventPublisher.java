package com.pengcheng.system.eventbus.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器。业务 Service 调 {@link #publish} 即可，不需要知道下游消费者。
 *
 * <p>下游通过 {@link org.springframework.context.event.EventListener @EventListener}
 * 订阅；Webhook 模块的 {@code WebhookEventListener} 会把事件转为 webhook_delivery。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public void publish(DomainEvent event) {
        if (event == null || event.getEventCode() == null) {
            log.warn("[DomainEvent] publish 入参非法，已忽略");
            return;
        }
        springPublisher.publishEvent(event);
        log.debug("[DomainEvent] published {} tenantId={} eventId={}",
                event.getEventCode(), event.getTenantId(), event.getEventId());
    }
}
