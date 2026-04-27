package com.pengcheng.job.task;

import com.pengcheng.system.saas.service.SaasBillService;
import com.pengcheng.system.saas.service.TenantSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * SaaS 计费定时任务。
 * <ul>
 *   <li>每月 1 日 02:00 — 生成上月账单（占位实现）；</li>
 *   <li>每日 03:00 — 扫描过期订阅置 EXPIRED。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pengcheng.saas.enabled", havingValue = "true", matchIfMissing = false)
public class SaasBillingJob {

    private final TenantSubscriptionService subscriptionService;
    private final SaasBillService billService;

    /** 每天凌晨 3 点扫过期订阅。 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void sweepExpired() {
        try {
            subscriptionService.expireDailySweep();
        } catch (Exception e) {
            log.error("[SaasBillingJob] expireDailySweep 异常", e);
        }
    }

    /** 每月 1 日 凌晨 2 点生成上月账单。 */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyBills() {
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
        try {
            int n = billService.generateBills(lastMonth);
            log.info("[SaasBillingJob] generateBills({}) generated {} bills", lastMonth, n);
        } catch (Exception e) {
            log.error("[SaasBillingJob] generateBills 异常", e);
        }
    }
}
