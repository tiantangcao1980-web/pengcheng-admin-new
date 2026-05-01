package com.pengcheng.realty.dashboard.cards;

import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.hr.performance.mapper.KpiScoreMapper;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.CustomerPoolEventLogMapper;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.realty.receivable.mapper.ReceivablePlanMapper;
import com.pengcheng.realty.receivable.mapper.ReceivableRecordMapper;
import com.pengcheng.system.calendar.mapper.CalendarEventMapper;
import com.pengcheng.realty.dashboard.cards.customer.CustomerHealthCardProvider;
import com.pengcheng.realty.dashboard.cards.customer.CustomerPoolCardProvider;
import com.pengcheng.realty.dashboard.cards.customer.CustomerSourceDistributionProvider;
import com.pengcheng.realty.dashboard.cards.customer.CustomerVisitCountCardProvider;
import com.pengcheng.realty.dashboard.cards.customer.NewCustomerTrendCardProvider;
import com.pengcheng.realty.dashboard.cards.finance.FinanceCommissionCardProvider;
import com.pengcheng.realty.dashboard.cards.finance.FinanceOverdueCardProvider;
import com.pengcheng.realty.dashboard.cards.finance.FinanceReceivableCardProvider;
import com.pengcheng.realty.dashboard.cards.general.MeetingTodayCardProvider;
import com.pengcheng.realty.dashboard.cards.general.TodoOverviewCardProvider;
import com.pengcheng.realty.dashboard.cards.sales.SalesActivityHeatmapCardProvider;
import com.pengcheng.realty.dashboard.cards.sales.SalesAvgDealValueCardProvider;
import com.pengcheng.realty.dashboard.cards.sales.SalesConversionRateCardProvider;
import com.pengcheng.realty.dashboard.cards.sales.SalesFunnelCardProvider;
import com.pengcheng.realty.dashboard.cards.sales.SalesPipelineValueCardProvider;
import com.pengcheng.realty.dashboard.cards.team.TeamApprovalPendingCardProvider;
import com.pengcheng.realty.dashboard.cards.team.TeamAttendanceCardProvider;
import com.pengcheng.realty.dashboard.cards.team.TeamGoalProgressCardProvider;
import com.pengcheng.realty.dashboard.cards.team.TeamLeaveCardProvider;
import com.pengcheng.realty.dashboard.cards.team.TeamSalesRankCardProvider;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import com.pengcheng.system.todo.mapper.TodoMapper;
import com.pengcheng.system.visit.mapper.SalesVisitMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 20 张预置看板卡片冒烟批量测试
 * <p>每张卡片：① metadata() 非空验证；② render(ctx) 不抛异常且返回非 null
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CardSmokeBatch: 20 预置卡片冒烟测试")
class CardSmokeBatchTest {

    // --- Mapper mocks ---
    @Mock RealtyCustomerMapper realtyCustomerMapper;
    @Mock CustomerDealMapper customerDealMapper;
    @Mock CustomerVisitMapper customerVisitMapper;
    @Mock CustomerPoolEventLogMapper customerPoolEventLogMapper;
    @Mock SalesVisitMapper salesVisitMapper;
    @Mock CommissionMapper commissionMapper;
    @Mock ReceivablePlanMapper receivablePlanMapper;
    @Mock ReceivableRecordMapper receivableRecordMapper;
    @Mock AttendanceRecordMapper attendanceRecordMapper;
    @Mock LeaveRequestMapper leaveRequestMapper;
    @Mock KpiScoreMapper kpiScoreMapper;
    @Mock PaymentRequestMapper paymentRequestMapper;
    @Mock TodoMapper todoMapper;
    @Mock CalendarEventMapper calendarEventMapper;

    private List<DashboardCardProvider> allProviders;
    private DashboardCardProvider.DashboardCardContext ctx;

    @BeforeEach
    void setUp() {
        // 默认 mock 返回空集合 / 零值
        when(realtyCustomerMapper.selectCount(any())).thenReturn(0L);
        when(customerDealMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(customerVisitMapper.selectCount(any())).thenReturn(0L);
        when(customerPoolEventLogMapper.selectCount(any())).thenReturn(0L);
        when(salesVisitMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(commissionMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivablePlanMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivablePlanMapper.selectCount(any())).thenReturn(0L);
        when(receivableRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(attendanceRecordMapper.selectCount(any())).thenReturn(0L);
        when(leaveRequestMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(kpiScoreMapper.selectOne(any())).thenReturn(null);
        when(paymentRequestMapper.selectCount(any())).thenReturn(0L);
        when(todoMapper.countPending(any())).thenReturn(0);
        when(calendarEventMapper.findByUserAndRange(any(), any(), any())).thenReturn(Collections.emptyList());

        // 构造所有 20 个 provider
        allProviders = buildAllProviders();

        // 构造上下文
        ctx = new DashboardCardProvider.DashboardCardContext() {
            public Long userId()                   { return 1L; }
            public Long tenantId()                 { return 1L; }
            public LocalDateTime windowStart()     { return LocalDateTime.now().minusDays(30); }
            public LocalDateTime windowEnd()       { return LocalDateTime.now(); }
            public Map<String, Object> params()    { return Map.of(); }
        };
    }

    @Test
    @DisplayName("所有卡片 metadata 字段非空")
    void all_cards_metadata_not_null() {
        for (DashboardCardProvider p : allProviders) {
            String prefix = "[" + p.code() + "] ";
            assertThat(p.code()).as(prefix + "code").isNotBlank();

            DashboardCardProvider.DashboardCardMetadata m = p.metadata();
            assertThat(m).as(prefix + "metadata").isNotNull();
            assertThat(m.name()).as(prefix + "name").isNotBlank();
            assertThat(m.category()).as(prefix + "category").isNotBlank();
            assertThat(m.applicableRoles()).as(prefix + "applicableRoles").isNotNull();
            assertThat(m.suggestedChart()).as(prefix + "suggestedChart").isNotBlank();
            assertThat(m.description()).as(prefix + "description").isNotBlank();
            assertThat(m.defaultCols()).as(prefix + "defaultCols").isBetween(1, 12);
            assertThat(m.defaultRows()).as(prefix + "defaultRows").isBetween(1, 10);
        }
    }

    @Test
    @DisplayName("所有卡片 render 不抛异常且返回非 null")
    void all_cards_render_returns_non_null() {
        for (DashboardCardProvider p : allProviders) {
            Object result = p.render(ctx);
            assertThat(result).as("[" + p.code() + "] render result").isNotNull();
        }
    }

    @Test
    @DisplayName("共 20 张卡片 code 全部唯一")
    void all_codes_are_unique() {
        long distinct = allProviders.stream().map(DashboardCardProvider::code).distinct().count();
        assertThat(distinct).isEqualTo(20);
    }

    // ---------------------------------------------------------------- 构造方法

    private List<DashboardCardProvider> buildAllProviders() {
        List<DashboardCardProvider> list = new ArrayList<>();

        // 销售域
        list.add(new SalesFunnelCardProvider(realtyCustomerMapper));
        list.add(new SalesPipelineValueCardProvider(realtyCustomerMapper));
        list.add(new SalesActivityHeatmapCardProvider(salesVisitMapper));
        list.add(new SalesConversionRateCardProvider(realtyCustomerMapper));
        list.add(new SalesAvgDealValueCardProvider(customerDealMapper));

        // 客户域
        list.add(new CustomerHealthCardProvider(realtyCustomerMapper));
        list.add(new CustomerPoolCardProvider(realtyCustomerMapper, customerPoolEventLogMapper));
        list.add(new CustomerVisitCountCardProvider(customerVisitMapper));
        list.add(new NewCustomerTrendCardProvider(realtyCustomerMapper));
        list.add(new CustomerSourceDistributionProvider(realtyCustomerMapper));

        // 团队域
        list.add(new TeamSalesRankCardProvider(customerDealMapper));
        list.add(new TeamAttendanceCardProvider(attendanceRecordMapper));
        list.add(new TeamLeaveCardProvider(leaveRequestMapper));
        list.add(new TeamApprovalPendingCardProvider(paymentRequestMapper));
        list.add(new TeamGoalProgressCardProvider(kpiScoreMapper));

        // 财务域
        list.add(new FinanceCommissionCardProvider(commissionMapper));
        list.add(new FinanceReceivableCardProvider(receivablePlanMapper, receivableRecordMapper));
        list.add(new FinanceOverdueCardProvider(receivablePlanMapper));

        // 通用域
        list.add(new TodoOverviewCardProvider(todoMapper));
        list.add(new MeetingTodayCardProvider(calendarEventMapper));

        return list;
    }
}
