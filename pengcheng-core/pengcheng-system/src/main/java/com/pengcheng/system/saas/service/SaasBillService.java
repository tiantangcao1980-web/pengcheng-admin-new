package com.pengcheng.system.saas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.system.saas.entity.SaasBill;
import com.pengcheng.system.saas.entity.SaasPlan;
import com.pengcheng.system.saas.entity.SaasUsageMetric;
import com.pengcheng.system.saas.entity.TenantSubscription;
import com.pengcheng.system.saas.mapper.SaasBillMapper;
import com.pengcheng.system.saas.mapper.SaasPlanMapper;
import com.pengcheng.system.saas.mapper.SaasUsageMetricMapper;
import com.pengcheng.system.saas.mapper.TenantSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaasBillService {

    /** API 超量单价：0.0001 元/次 */
    private static final BigDecimal API_OVERAGE_UNIT = new BigDecimal("0.0001");
    /** 存储超量单价：1 元/GB/月 */
    private static final BigDecimal STORAGE_OVERAGE_UNIT = new BigDecimal("1");

    private final SaasBillMapper mapper;
    private final TenantSubscriptionMapper subscriptionMapper;
    private final SaasPlanMapper planMapper;
    private final SaasUsageMetricMapper usageMetricMapper;

    /**
     * 为指定月份批量生成账单。
     *
     * @param yyyymm 账期，格式 "yyyyMM"（如 "202503"）
     * @return 本次新生成的账单数（已存在则跳过，幂等）
     */
    public int generateBills(String yyyymm) {
        // 解析账期首尾日
        YearMonth ym = YearMonth.parse(yyyymm, DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd   = ym.atEndOfMonth();

        // 扫所有 ACTIVE 且 end_date >= periodStart 的订阅
        List<TenantSubscription> subs = subscriptionMapper.selectList(
                new LambdaQueryWrapper<TenantSubscription>()
                        .eq(TenantSubscription::getStatus, TenantSubscription.STATUS_ACTIVE)
                        .le(TenantSubscription::getStartDate, periodEnd)
        );

        int count = 0;
        for (TenantSubscription sub : subs) {
            try {
                if (generateBillForSubscription(sub, yyyymm, periodStart, periodEnd)) {
                    count++;
                }
            } catch (Exception e) {
                log.error("[SaasBill] 生成账单异常 subscriptionId={} tenantId={}",
                        sub.getId(), sub.getTenantId(), e);
            }
        }
        log.info("[SaasBill] generateBills({}) generated {} bills (total subs={})", yyyymm, count, subs.size());
        return count;
    }

    /**
     * 为单个订阅生成账单；已存在则跳过（幂等）。
     *
     * @return true 表示新建了账单，false 表示已存在跳过
     */
    private boolean generateBillForSubscription(TenantSubscription sub,
                                                 String yyyymm,
                                                 LocalDate periodStart,
                                                 LocalDate periodEnd) {
        Long tenantId = sub.getTenantId();

        // 幂等检查：同 tenantId + subscriptionId + period_start 已存在则跳过
        long existing = mapper.selectCount(new LambdaQueryWrapper<SaasBill>()
                .eq(SaasBill::getTenantId, tenantId)
                .eq(SaasBill::getSubscriptionId, sub.getId())
                .eq(SaasBill::getPeriodStart, periodStart));
        if (existing > 0) {
            log.debug("[SaasBill] 幂等跳过 tenantId={} period={}", tenantId, yyyymm);
            return false;
        }

        // 拿套餐
        SaasPlan plan = planMapper.selectById(sub.getPlanId());
        if (plan == null) {
            log.warn("[SaasBill] plan not found planId={}, skip", sub.getPlanId());
            return false;
        }

        // base_amount = plan.price_per_month
        BigDecimal baseAmount = plan.getPricePerMonth();

        // overage 计算
        BigDecimal overageAmount = calcOverage(tenantId, yyyymm, plan);

        BigDecimal totalAmount = baseAmount.add(overageAmount);

        // bill_no = "BILL-yyyymm-tenantId-uuid8"
        String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String billNo = "BILL-" + yyyymm + "-" + tenantId + "-" + uuid8;

        SaasBill bill = new SaasBill();
        bill.setBillNo(billNo);
        bill.setTenantId(tenantId);
        bill.setSubscriptionId(sub.getId());
        bill.setPeriodStart(periodStart);
        bill.setPeriodEnd(periodEnd);
        bill.setBaseAmount(baseAmount);
        bill.setOverageAmount(overageAmount);
        bill.setTotalAmount(totalAmount);
        bill.setStatus(SaasBill.STATUS_UNPAID);

        mapper.insert(bill);
        log.info("[SaasBill] created bill={} tenantId={} total={}", billNo, tenantId, totalAmount);
        return true;
    }

    /**
     * 计算超量费用：API_CALLS + STORAGE_GB。
     */
    private BigDecimal calcOverage(Long tenantId, String yyyymm, SaasPlan plan) {
        BigDecimal apiFee = overageFee(
                getUsageValue(tenantId, SaasUsageMetric.TYPE_API_CALLS, yyyymm),
                plan.getMaxApiCallsPerMonth(),
                API_OVERAGE_UNIT);
        BigDecimal storageFee = overageFee(
                getUsageValue(tenantId, SaasUsageMetric.TYPE_STORAGE_GB, yyyymm),
                plan.getMaxStorageGb(),
                STORAGE_OVERAGE_UNIT);
        return apiFee.add(storageFee);
    }

    /** 单项超量费 = max(usage - quota, 0) * unitPrice；quota 为 null 或 ≤0 时不收费。 */
    private static BigDecimal overageFee(long usage, Integer quota, BigDecimal unitPrice) {
        if (quota == null || quota <= 0 || usage <= quota) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(usage - quota).multiply(unitPrice);
    }

    /**
     * 查用量值，不存在时返回 0。
     */
    private long getUsageValue(Long tenantId, String metricType, String yyyymm) {
        SaasUsageMetric metric = usageMetricMapper.selectOne(
                new LambdaQueryWrapper<SaasUsageMetric>()
                        .eq(SaasUsageMetric::getTenantId, tenantId)
                        .eq(SaasUsageMetric::getMetricType, metricType)
                        .eq(SaasUsageMetric::getPeriodYyyymm, yyyymm));
        return metric == null ? 0L : metric.getValueNum();
    }

    /**
     * 将 period_end < (今天 - 30 天) 且 UNPAID 的账单置为 OVERDUE。
     */
    public void markOverdue() {
        LocalDate threshold = LocalDate.now().minusDays(30);
        int n = mapper.update(null, new LambdaUpdateWrapper<SaasBill>()
                .eq(SaasBill::getStatus, SaasBill.STATUS_UNPAID)
                .lt(SaasBill::getPeriodEnd, threshold)
                .set(SaasBill::getStatus, SaasBill.STATUS_OVERDUE));
        if (n > 0) {
            log.info("[SaasBill] markOverdue: {} bills marked OVERDUE (threshold={})", n, threshold);
        }
    }

    public void payBill(Long id) {
        SaasBill bill = mapper.selectById(id);
        if (bill == null) throw new IllegalArgumentException("bill not found");
        bill.setStatus(SaasBill.STATUS_PAID);
        bill.setPaidAt(LocalDateTime.now());
        mapper.updateById(bill);
    }

    public List<SaasBill> findUnpaid(Long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<SaasBill>()
                .eq(SaasBill::getTenantId, tenantId)
                .in(SaasBill::getStatus, SaasBill.STATUS_UNPAID, SaasBill.STATUS_OVERDUE)
                .orderByDesc(SaasBill::getPeriodEnd));
    }
}
