package com.pengcheng.system.eventbus.webhook.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.eventbus.event.DomainEvent;
import com.pengcheng.system.eventbus.webhook.entity.WebhookDelivery;
import com.pengcheng.system.eventbus.webhook.entity.WebhookSubscription;
import com.pengcheng.system.eventbus.webhook.mapper.WebhookDeliveryMapper;
import com.pengcheng.system.eventbus.webhook.mapper.WebhookSubscriptionMapper;
import com.pengcheng.system.eventbus.webhook.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryServiceImpl implements WebhookDeliveryService {

    /** HTTP 投递超时（毫秒）。 */
    private static final int HTTP_TIMEOUT_MS = 15_000;
    /** 响应体最大保留字节数（超长截断）。 */
    private static final int RESPONSE_BODY_MAX_LEN = 1000;
    /** 最大重试次数；超过即标记 DEAD。 */
    private static final int MAX_ATTEMPTS = 5;

    private final WebhookSubscriptionMapper subscriptionMapper;
    private final WebhookDeliveryMapper deliveryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void enqueueForEvent(DomainEvent event) {
        if (event.getTenantId() == null) {
            log.debug("[Webhook] event {} 无 tenantId，跳过订阅匹配", event.getEventCode());
            return;
        }
        List<WebhookSubscription> subs = subscriptionMapper.findEnabledByEvent(
                event.getTenantId(), event.getEventCode());
        if (subs.isEmpty()) return;

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(Map.of(
                    "eventId", event.getEventId(),
                    "eventCode", event.getEventCode(),
                    "occurredAt", event.getOccurredAt().toString(),
                    "tenantId", event.getTenantId(),
                    "data", event.getPayload()
            ));
        } catch (Exception e) {
            log.error("[Webhook] payload 序列化失败 eventId={}", event.getEventId(), e);
            return;
        }

        for (WebhookSubscription sub : subs) {
            WebhookDelivery d = new WebhookDelivery();
            d.setSubscriptionId(sub.getId());
            d.setTenantId(sub.getTenantId());
            d.setEventCode(event.getEventCode());
            d.setEventId(event.getEventId());
            d.setPayload(payloadJson);
            d.setRequestUrl(sub.getUrl());
            d.setStatus(WebhookDelivery.STATUS_PENDING);
            d.setAttemptCount(0);
            d.setNextAttemptAt(LocalDateTime.now());
            deliveryMapper.insert(d);
        }
    }

    @Override
    public boolean attemptDeliver(Long deliveryId) {
        WebhookDelivery d = deliveryMapper.selectById(deliveryId);
        if (d == null) return false;
        WebhookSubscription sub = subscriptionMapper.selectById(d.getSubscriptionId());
        if (sub == null || !Integer.valueOf(1).equals(sub.getEnabled())) {
            d.setStatus(WebhookDelivery.STATUS_FAILED);
            d.setErrorMsg("subscription disabled or removed");
            deliveryMapper.updateById(d);
            return false;
        }

        // HMAC-SHA256 签名
        String signature;
        try {
            HMac mac = new HMac(HmacAlgorithm.HmacSHA256, sub.getSecret().getBytes(StandardCharsets.UTF_8));
            signature = Base64.encode(mac.digest(d.getPayload()));
        } catch (Exception e) {
            log.error("[Webhook] 签名计算失败 deliveryId={}", deliveryId, e);
            return false;
        }

        d.setAttemptCount(d.getAttemptCount() + 1);
        d.setLastAttemptAt(LocalDateTime.now());

        try (HttpResponse resp = HttpRequest.post(d.getRequestUrl())
                .body(d.getPayload())
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", signature)
                .header("X-Webhook-Event", d.getEventCode())
                .header("X-Webhook-Event-Id", d.getEventId())
                .timeout(HTTP_TIMEOUT_MS)
                .execute()) {
            d.setResponseStatus(resp.getStatus());
            String body = resp.body();
            d.setResponseBody(body != null && body.length() > RESPONSE_BODY_MAX_LEN
                    ? body.substring(0, RESPONSE_BODY_MAX_LEN) : body);

            if (resp.getStatus() >= 200 && resp.getStatus() < 300) {
                d.setStatus(WebhookDelivery.STATUS_SUCCESS);
                deliveryMapper.updateById(d);
                return true;
            }
            d.setErrorMsg("non-2xx: " + resp.getStatus());
        } catch (Exception e) {
            d.setErrorMsg(e.getClass().getSimpleName() + ": " + e.getMessage());
            log.warn("[Webhook] 投递异常 deliveryId={} attempt={}: {}",
                    deliveryId, d.getAttemptCount(), e.getMessage());
        }

        // 失败：算下次重试或 DEAD
        if (d.getAttemptCount() >= MAX_ATTEMPTS) {
            d.setStatus(WebhookDelivery.STATUS_DEAD);
        } else {
            long backoff = WebhookDeliveryService.backoffSecondsByAttempt(d.getAttemptCount());
            d.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff));
        }
        deliveryMapper.updateById(d);
        return false;
    }

    @Override
    public void replayDead(Long deliveryId) {
        WebhookDelivery d = deliveryMapper.selectById(deliveryId);
        if (d == null) return;
        d.setStatus(WebhookDelivery.STATUS_PENDING);
        d.setAttemptCount(0);
        d.setNextAttemptAt(LocalDateTime.now());
        d.setErrorMsg(null);
        deliveryMapper.updateById(d);
    }
}
