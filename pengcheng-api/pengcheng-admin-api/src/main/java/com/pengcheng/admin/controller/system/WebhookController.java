package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.eventbus.webhook.entity.WebhookDelivery;
import com.pengcheng.system.eventbus.webhook.entity.WebhookEventType;
import com.pengcheng.system.eventbus.webhook.entity.WebhookSubscription;
import com.pengcheng.system.eventbus.webhook.mapper.WebhookDeliveryMapper;
import com.pengcheng.system.eventbus.webhook.mapper.WebhookEventTypeMapper;
import com.pengcheng.system.eventbus.webhook.mapper.WebhookSubscriptionMapper;
import com.pengcheng.system.eventbus.webhook.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Webhook 订阅与投递管理（管理员视角）。
 */
@RestController
@RequestMapping("/admin/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSubscriptionMapper subscriptionMapper;
    private final WebhookDeliveryMapper deliveryMapper;
    private final WebhookEventTypeMapper eventTypeMapper;
    private final WebhookDeliveryService deliveryService;

    /* ========== 事件类型 ========== */

    @GetMapping("/events")
    @SaCheckPermission("webhook:list")
    public Result<List<WebhookEventType>> listEventTypes() {
        return Result.ok(eventTypeMapper.selectList(
                new LambdaQueryWrapper<WebhookEventType>().eq(WebhookEventType::getEnabled, 1)));
    }

    /* ========== 订阅 CRUD ========== */

    @GetMapping("/subscriptions")
    @SaCheckPermission("webhook:list")
    public Result<List<WebhookSubscription>> listSubscriptions(@RequestParam Long tenantId) {
        return Result.ok(subscriptionMapper.selectList(
                new LambdaQueryWrapper<WebhookSubscription>().eq(WebhookSubscription::getTenantId, tenantId)));
    }

    @PostMapping("/subscriptions")
    @SaCheckPermission("webhook:manage")
    public Result<Long> createSubscription(@RequestBody WebhookSubscription sub) {
        sub.setEnabled(1);
        sub.setFailureCount(0);
        subscriptionMapper.insert(sub);
        return Result.ok(sub.getId());
    }

    @PutMapping("/subscriptions/{id}")
    @SaCheckPermission("webhook:manage")
    public Result<Void> updateSubscription(@PathVariable Long id, @RequestBody WebhookSubscription sub) {
        sub.setId(id);
        subscriptionMapper.updateById(sub);
        return Result.ok();
    }

    @DeleteMapping("/subscriptions/{id}")
    @SaCheckPermission("webhook:manage")
    public Result<Void> deleteSubscription(@PathVariable Long id) {
        subscriptionMapper.deleteById(id);
        return Result.ok();
    }

    /* ========== 投递记录 ========== */

    @GetMapping("/deliveries")
    @SaCheckPermission("webhook:list")
    public Result<List<WebhookDelivery>> listDeliveries(@RequestParam Long subscriptionId,
                                                        @RequestParam(required = false) String status) {
        LambdaQueryWrapper<WebhookDelivery> q = new LambdaQueryWrapper<WebhookDelivery>()
                .eq(WebhookDelivery::getSubscriptionId, subscriptionId);
        if (status != null) q.eq(WebhookDelivery::getStatus, status);
        q.orderByDesc(WebhookDelivery::getId).last("LIMIT 200");
        return Result.ok(deliveryMapper.selectList(q));
    }

    @PostMapping("/deliveries/{id}/replay")
    @SaCheckPermission("webhook:manage")
    public Result<Void> replay(@PathVariable Long id) {
        deliveryService.replayDead(id);
        return Result.ok();
    }
}
