package com.pengcheng.system.saas;

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
import com.pengcheng.system.saas.service.SaasBillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaasBillService 账单生成 & 逾期标记")
class SaasBillServiceImplTest {

    @Mock private SaasBillMapper billMapper;
    @Mock private TenantSubscriptionMapper subscriptionMapper;
    @Mock private SaasPlanMapper planMapper;
    @Mock private SaasUsageMetricMapper usageMetricMapper;

    private SaasBillService service;

    /** 固定 insert mock 并返回 1 */
    @BeforeEach
    void setUp() {
        service = new SaasBillService(billMapper, subscriptionMapper, planMapper, usageMetricMapper);
        when(billMapper.insert(any(SaasBill.class))).thenReturn(1);
    }

    // -----------------------------------------------------------------------
    // 工具方法
    // -----------------------------------------------------------------------

    private SaasPlan proPlan() {
        SaasPlan plan = new SaasPlan();
        plan.setId(2L);
        plan.setCode("pro");
        plan.setPricePerMonth(new BigDecimal("299"));
        plan.setMaxApiCallsPerMonth(100_000);
        plan.setMaxStorageGb(50);
        plan.setMaxUsers(50);
        return plan;
    }

    private TenantSubscription activeSub(Long tenantId, Long planId) {
        TenantSubscription sub = new TenantSubscription();
        sub.setId(tenantId * 10);
        sub.setTenantId(tenantId);
        sub.setPlanId(planId);
        sub.setStatus(TenantSubscription.STATUS_ACTIVE);
        sub.setStartDate(LocalDate.of(2025, 1, 1));
        sub.setEndDate(LocalDate.of(2026, 1, 1));
        return sub;
    }

    private void noUsage() {
        when(usageMetricMapper.selectOne(any())).thenReturn(null);
    }

    private void stubUsage(Long tenantId, String type, String yyyymm, long value) {
        SaasUsageMetric m = new SaasUsageMetric();
        m.setTenantId(tenantId);
        m.setMetricType(type);
        m.setPeriodYyyymm(yyyymm);
        m.setValueNum(value);
        when(usageMetricMapper.selectOne(argThat(w -> {
            // 只要是 LambdaQueryWrapper 就接受（Mockito 无法直接比较 wrapper 内部条件）
            return w instanceof LambdaQueryWrapper;
        }))).thenAnswer(inv -> {
            // 按调用顺序返回对应 metric（利用 thenReturn 链式无法区分，改用 lenient doAnswer）
            return m;
        });
    }

    // -----------------------------------------------------------------------
    // 用例 1：单订阅生成 1 张账单，base=299，无 overage
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("单订阅生成 1 张账单，base=299 无超量")
    void generateBills_singleSub_noOverage() {
        TenantSubscription sub = activeSub(1001L, 2L);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));
        when(planMapper.selectById(2L)).thenReturn(proPlan());
        // 无已有账单
        when(billMapper.selectCount(any())).thenReturn(0L);
        noUsage();

        int count = service.generateBills("202503");

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<SaasBill> cap = ArgumentCaptor.forClass(SaasBill.class);
        verify(billMapper).insert(cap.capture());
        SaasBill bill = cap.getValue();
        assertThat(bill.getBaseAmount()).isEqualByComparingTo("299");
        assertThat(bill.getOverageAmount()).isEqualByComparingTo("0");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("299");
        assertThat(bill.getStatus()).isEqualTo(SaasBill.STATUS_UNPAID);
        assertThat(bill.getBillNo()).startsWith("BILL-202503-1001-");
        assertThat(bill.getPeriodStart()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(bill.getPeriodEnd()).isEqualTo(LocalDate.of(2025, 3, 31));
    }

    // -----------------------------------------------------------------------
    // 用例 2：多订阅生成多账单
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("多订阅生成多账单")
    void generateBills_multiSub() {
        TenantSubscription sub1 = activeSub(2001L, 2L);
        TenantSubscription sub2 = activeSub(2002L, 2L);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub1, sub2));
        when(planMapper.selectById(2L)).thenReturn(proPlan());
        when(billMapper.selectCount(any())).thenReturn(0L);
        noUsage();

        int count = service.generateBills("202503");

        assertThat(count).isEqualTo(2);
        verify(billMapper, times(2)).insert(any(SaasBill.class));
    }

    // -----------------------------------------------------------------------
    // 用例 3：API 超量计费 — max=100k 实际 150k → overage=5 元
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("API 超量计费：实际 150000 次 超过 100000 → overage=5 元")
    void generateBills_apiOverage() {
        TenantSubscription sub = activeSub(3001L, 2L);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));
        when(planMapper.selectById(2L)).thenReturn(proPlan());
        when(billMapper.selectCount(any())).thenReturn(0L);

        // API_CALLS=150000，STORAGE_GB=0
        SaasUsageMetric apiMetric = new SaasUsageMetric();
        apiMetric.setMetricType(SaasUsageMetric.TYPE_API_CALLS);
        apiMetric.setValueNum(150_000L);

        // 第一次调用（API_CALLS）返回有值，其余（STORAGE_GB、MAU）返回 null
        when(usageMetricMapper.selectOne(any()))
                .thenReturn(apiMetric)   // API_CALLS
                .thenReturn(null);       // STORAGE_GB

        int count = service.generateBills("202503");
        assertThat(count).isEqualTo(1);

        ArgumentCaptor<SaasBill> cap = ArgumentCaptor.forClass(SaasBill.class);
        verify(billMapper).insert(cap.capture());
        SaasBill bill = cap.getValue();
        // overage = (150000-100000) * 0.0001 = 5
        assertThat(bill.getOverageAmount()).isEqualByComparingTo("5.0000");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("304.0000");
    }

    // -----------------------------------------------------------------------
    // 用例 4：存储超量计费 — max=50GB 实际 70GB → overage=20 元
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("存储超量计费：70GB 超过 50GB → overage=20 元")
    void generateBills_storageOverage() {
        TenantSubscription sub = activeSub(4001L, 2L);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));
        when(planMapper.selectById(2L)).thenReturn(proPlan());
        when(billMapper.selectCount(any())).thenReturn(0L);

        SaasUsageMetric storageMetric = new SaasUsageMetric();
        storageMetric.setMetricType(SaasUsageMetric.TYPE_STORAGE_GB);
        storageMetric.setValueNum(70L);

        // 第一次（API_CALLS）返回 null，第二次（STORAGE_GB）返回超量
        when(usageMetricMapper.selectOne(any()))
                .thenReturn(null)          // API_CALLS
                .thenReturn(storageMetric); // STORAGE_GB

        int count = service.generateBills("202503");
        assertThat(count).isEqualTo(1);

        ArgumentCaptor<SaasBill> cap = ArgumentCaptor.forClass(SaasBill.class);
        verify(billMapper).insert(cap.capture());
        SaasBill bill = cap.getValue();
        // overage = (70-50) * 1 = 20
        assertThat(bill.getOverageAmount()).isEqualByComparingTo("20");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("319");
    }

    // -----------------------------------------------------------------------
    // 用例 5：幂等 — 同 yyyymm 重跑不重复 INSERT
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("幂等：同账期重跑不重复 INSERT")
    void generateBills_idempotent() {
        TenantSubscription sub = activeSub(5001L, 2L);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));
        when(planMapper.selectById(2L)).thenReturn(proPlan());
        // selectCount 返回 1 → 已存在
        when(billMapper.selectCount(any())).thenReturn(1L);

        int count = service.generateBills("202503");

        assertThat(count).isEqualTo(0);
        verify(billMapper, never()).insert(any(SaasBill.class));
    }

    // -----------------------------------------------------------------------
    // 用例 6：markOverdue — 30 天前未付账单转 OVERDUE
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("markOverdue：逾期 30 天 UNPAID 账单转 OVERDUE")
    void markOverdue_updatesStatus() {
        when(billMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(3);

        service.markOverdue();

        verify(billMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
