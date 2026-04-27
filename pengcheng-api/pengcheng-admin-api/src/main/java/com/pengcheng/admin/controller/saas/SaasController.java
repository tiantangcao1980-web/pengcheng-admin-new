package com.pengcheng.admin.controller.saas;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.saas.entity.SaasBill;
import com.pengcheng.system.saas.entity.SaasPlan;
import com.pengcheng.system.saas.entity.TenantSubscription;
import com.pengcheng.system.saas.mapper.SaasPlanMapper;
import com.pengcheng.system.saas.service.SaasBillService;
import com.pengcheng.system.saas.service.TenantSubscriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 套餐 / 订阅 / 账单 管理（管理员视角）。
 */
@RestController
@RequestMapping("/admin/saas")
@RequiredArgsConstructor
public class SaasController {

    private final SaasPlanMapper planMapper;
    private final TenantSubscriptionService subscriptionService;
    private final SaasBillService billService;

    @GetMapping("/plans")
    @SaCheckPermission("saas:plan:list")
    public Result<List<SaasPlan>> listPlans() {
        return Result.ok(planMapper.selectList(
                new LambdaQueryWrapper<SaasPlan>().eq(SaasPlan::getEnabled, 1)));
    }

    @PostMapping("/subscriptions")
    @SaCheckPermission("saas:subscription:manage")
    public Result<TenantSubscription> subscribe(@RequestBody SubscribeReq req) {
        return Result.ok(subscriptionService.subscribe(
                req.getTenantId(), req.getPlanId(), req.getDurationMonths(), Boolean.TRUE.equals(req.getAutoRenew())));
    }

    @PostMapping("/subscriptions/{id}/cancel")
    @SaCheckPermission("saas:subscription:manage")
    public Result<Void> cancel(@PathVariable Long id) {
        subscriptionService.cancel(id);
        return Result.ok();
    }

    @PostMapping("/subscriptions/{id}/extend")
    @SaCheckPermission("saas:subscription:manage")
    public Result<Void> extend(@PathVariable Long id, @RequestParam int months) {
        subscriptionService.extend(id, months);
        return Result.ok();
    }

    @GetMapping("/subscriptions/active")
    @SaCheckPermission("saas:subscription:list")
    public Result<TenantSubscription> active(@RequestParam Long tenantId) {
        return Result.ok(subscriptionService.findActive(tenantId));
    }

    @GetMapping("/bills/unpaid")
    @SaCheckPermission("saas:bill:list")
    public Result<List<SaasBill>> unpaidBills(@RequestParam Long tenantId) {
        return Result.ok(billService.findUnpaid(tenantId));
    }

    @PostMapping("/bills/{id}/pay")
    @SaCheckPermission("saas:bill:manage")
    public Result<Void> payBill(@PathVariable Long id) {
        billService.payBill(id);
        return Result.ok();
    }

    @Data
    public static class SubscribeReq {
        private Long tenantId;
        private Long planId;
        private Integer durationMonths;
        private Boolean autoRenew;
    }
}
