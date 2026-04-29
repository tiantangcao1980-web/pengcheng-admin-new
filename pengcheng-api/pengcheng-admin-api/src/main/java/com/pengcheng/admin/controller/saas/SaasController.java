package com.pengcheng.admin.controller.saas;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.saas.entity.SaasBill;
import com.pengcheng.system.saas.entity.SaasPlan;
import com.pengcheng.system.saas.entity.SaasUsageMetric;
import com.pengcheng.system.saas.entity.TenantSubscription;
import com.pengcheng.system.saas.mapper.SaasPlanMapper;
import com.pengcheng.system.saas.mapper.SaasUsageMetricMapper;
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
    private final SaasUsageMetricMapper usageMetricMapper;
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

    /**
     * 查询指定租户指定月份的用量，并与套餐限额对比。
     * GET /admin/saas/usage?tenantId=&yyyymm=
     */
    @GetMapping("/usage")
    @SaCheckPermission("saas:usage:query")
    public Result<UsageResp> queryUsage(@RequestParam Long tenantId, @RequestParam String yyyymm) {
        // 查活跃订阅 -> plan
        TenantSubscription sub = subscriptionService.findActive(tenantId);
        SaasPlan plan = sub != null ? planMapper.selectById(sub.getPlanId()) : null;

        // 查三种计量
        long apiCalls  = getMetricValue(tenantId, SaasUsageMetric.TYPE_API_CALLS,  yyyymm);
        long storageGb = getMetricValue(tenantId, SaasUsageMetric.TYPE_STORAGE_GB, yyyymm);
        long mau       = getMetricValue(tenantId, SaasUsageMetric.TYPE_MAU,        yyyymm);

        UsageResp resp = new UsageResp();
        resp.setTenantId(tenantId);
        resp.setYyyymm(yyyymm);
        resp.setApiCalls(apiCalls);
        resp.setStorageGb(storageGb);
        resp.setMau(mau);

        if (plan != null) {
            resp.setPlanCode(plan.getCode());
            resp.setMaxApiCalls((long) plan.getMaxApiCallsPerMonth());
            resp.setMaxStorageGb((long) plan.getMaxStorageGb());
            resp.setMaxUsers((long) plan.getMaxUsers());
            resp.setApiOverage(plan.getMaxApiCallsPerMonth() > 0 && apiCalls > plan.getMaxApiCallsPerMonth());
            resp.setStorageOverage(plan.getMaxStorageGb() > 0 && storageGb > plan.getMaxStorageGb());
        }
        return Result.ok(resp);
    }

    private long getMetricValue(Long tenantId, String metricType, String yyyymm) {
        SaasUsageMetric m = usageMetricMapper.selectOne(
                new LambdaQueryWrapper<SaasUsageMetric>()
                        .eq(SaasUsageMetric::getTenantId, tenantId)
                        .eq(SaasUsageMetric::getMetricType, metricType)
                        .eq(SaasUsageMetric::getPeriodYyyymm, yyyymm));
        return m == null ? 0L : m.getValueNum();
    }

    @Data
    public static class SubscribeReq {
        private Long tenantId;
        private Long planId;
        private Integer durationMonths;
        private Boolean autoRenew;
    }

    @Data
    public static class UsageResp {
        private Long tenantId;
        private String yyyymm;
        private String planCode;
        private long apiCalls;
        private long storageGb;
        private long mau;
        private Long maxApiCalls;
        private Long maxStorageGb;
        private Long maxUsers;
        private Boolean apiOverage;
        private Boolean storageOverage;
    }
}
