package com.pengcheng.job.task;

import com.pengcheng.system.eventbus.webhook.entity.WebhookDelivery;
import com.pengcheng.system.eventbus.webhook.mapper.WebhookDeliveryMapper;
import com.pengcheng.system.eventbus.webhook.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Webhook 投递重试 Job — 每分钟扫一次可重试的 PENDING 投递。
 *
 * <p>开关：{@code pengcheng.webhook.retry-enabled=false} 可禁用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "pengcheng.webhook.retry-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WebhookRetryJob {

    private final WebhookDeliveryMapper deliveryMapper;
    private final WebhookDeliveryService deliveryService;

    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 60 * 1000L, initialDelay = 30 * 1000L)
    public void retry() {
        try {
            List<WebhookDelivery> retryable = deliveryMapper.findRetryable(LocalDateTime.now(), BATCH_SIZE);
            if (retryable.isEmpty()) return;
            log.info("[WebhookRetryJob] 扫描到 {} 条可重试投递", retryable.size());
            int success = 0, fail = 0;
            for (WebhookDelivery d : retryable) {
                if (deliveryService.attemptDeliver(d.getId())) {
                    success++;
                } else {
                    fail++;
                }
            }
            log.info("[WebhookRetryJob] 完成：success={} fail={}", success, fail);
        } catch (Exception e) {
            log.error("[WebhookRetryJob] 异常", e);
        }
    }
}
