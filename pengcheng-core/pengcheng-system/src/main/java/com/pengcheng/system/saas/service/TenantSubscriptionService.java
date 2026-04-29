package com.pengcheng.system.saas.service;

import com.pengcheng.system.saas.entity.TenantSubscription;
import com.pengcheng.system.saas.mapper.TenantSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSubscriptionService {

    private final TenantSubscriptionMapper mapper;

    public TenantSubscription subscribe(Long tenantId, Long planId, int durationMonths, boolean autoRenew) {
        TenantSubscription sub = new TenantSubscription();
        sub.setTenantId(tenantId);
        sub.setPlanId(planId);
        sub.setStatus(TenantSubscription.STATUS_ACTIVE);
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(durationMonths));
        sub.setAutoRenew(autoRenew ? 1 : 0);
        mapper.insert(sub);
        return sub;
    }

    public void cancel(Long id) {
        TenantSubscription sub = mapper.selectById(id);
        if (sub == null) return;
        sub.setStatus(TenantSubscription.STATUS_CANCELLED);
        mapper.updateById(sub);
    }

    public void extend(Long id, int months) {
        TenantSubscription sub = mapper.selectById(id);
        if (sub == null) throw new IllegalArgumentException("subscription not found: " + id);
        sub.setEndDate(sub.getEndDate().plusMonths(months));
        if (TenantSubscription.STATUS_EXPIRED.equals(sub.getStatus())) {
            sub.setStatus(TenantSubscription.STATUS_ACTIVE);
        }
        mapper.updateById(sub);
    }

    public TenantSubscription findActive(Long tenantId) {
        return mapper.findActive(tenantId);
    }

    /** 由 SaasBillingJob 每日凌晨调用。 */
    public int expireDailySweep() {
        int n = mapper.sweepExpired(LocalDate.now());
        if (n > 0) log.info("[Saas] expireDailySweep affected {} subscriptions", n);
        return n;
    }
}
