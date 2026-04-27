package com.pengcheng.system.eventbus.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 领域事件（业务方调 DomainEventPublisher.publish 发出）。
 *
 * <p>eventCode 命名约定：{aggregate}.{action}，例如 customer.created / deal.signed。
 * payload 通常是关键业务字段的扁平 Map（不要塞整个实体，避免无限循环序列化）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    private String eventCode;
    private Long tenantId;
    private String eventId;             // 不传则自动生成 UUID
    private LocalDateTime occurredAt;   // 不传则用 now()
    private Map<String, Object> payload;

    public static DomainEvent of(String eventCode, Long tenantId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventCode(eventCode)
                .tenantId(tenantId)
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build();
    }
}
