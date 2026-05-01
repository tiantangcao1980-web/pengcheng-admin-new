package com.pengcheng.realty.dashboard;

import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.unit.mapper.RealtyUnitMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 房产专属 3 张看板卡片冒烟测试（Smoke）
 *
 * <p>验证：① code 唯一；② name/category 非空；③ render(ctx) 不抛且返回非 null
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RealtyCardSmoke: 3 张房产卡片冒烟测试")
class RealtyCardSmokeTest {

    @Mock RealtyUnitMapper realtyUnitMapper;
    @Mock RealtyCustomerMapper realtyCustomerMapper;
    @Mock CustomerDealMapper customerDealMapper;
    @Mock AllianceMapper allianceMapper;
    @Mock CommissionMapper commissionMapper;

    private List<DashboardCardProvider> providers;
    private DashboardCardProvider.DashboardCardContext ctx;

    @BeforeEach
    void setUp() {
        // 默认返回空 / 零值，确保 render 不因 NPE 中断
        when(realtyUnitMapper.selectCount(any())).thenReturn(0L);
        when(realtyCustomerMapper.selectCount(any())).thenReturn(0L);
        when(realtyCustomerMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(customerDealMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(allianceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(commissionMapper.selectList(any())).thenReturn(Collections.emptyList());

        providers = List.of(
                new UnitSalesRateCardProvider(realtyUnitMapper),
                new VisitFunnelCardProvider(realtyCustomerMapper, customerDealMapper),
                new ChannelRoiCardProvider(allianceMapper, realtyCustomerMapper, customerDealMapper, commissionMapper)
        );

        ctx = new DashboardCardProvider.DashboardCardContext() {
            public Long userId()               { return 1L; }
            public Long tenantId()             { return 1L; }
            public LocalDateTime windowStart() { return LocalDateTime.now().minusDays(30); }
            public LocalDateTime windowEnd()   { return LocalDateTime.now(); }
            public Map<String, Object> params(){ return Map.of(); }
        };
    }

    @Test
    @DisplayName("3 张房产卡片 code 唯一、metadata 非空、render 返回非 null")
    void realty_cards_smoke_batch() {
        // code 唯一性
        long distinct = providers.stream().map(DashboardCardProvider::code).distinct().count();
        assertThat(distinct).as("code 应全部唯一").isEqualTo(providers.size());

        // 逐张验证
        for (DashboardCardProvider p : providers) {
            String prefix = "[" + p.code() + "] ";

            // metadata 字段非空
            DashboardCardProvider.DashboardCardMetadata m = p.metadata();
            assertThat(m).as(prefix + "metadata").isNotNull();
            assertThat(m.name()).as(prefix + "name").isNotBlank();
            assertThat(m.category()).as(prefix + "category").isNotBlank();
            assertThat(m.applicableRoles()).as(prefix + "applicableRoles").isNotNull();
            assertThat(m.suggestedChart()).as(prefix + "suggestedChart").isNotBlank();
            assertThat(m.defaultCols()).as(prefix + "defaultCols").isBetween(1, 12);
            assertThat(m.defaultRows()).as(prefix + "defaultRows").isBetween(1, 10);

            // render 不抛且非 null
            Object result = p.render(ctx);
            assertThat(result).as(prefix + "render result").isNotNull();
        }
    }

    @Test
    @DisplayName("realty.unit-sales-rate 验证 code 正确")
    void unit_sales_rate_code() {
        DashboardCardProvider p = providers.stream()
                .filter(c -> "realty.unit-sales-rate".equals(c.code()))
                .findFirst().orElseThrow();
        assertThat(p.metadata().category()).isEqualTo("realty");
        assertThat(p.metadata().suggestedChart()).isEqualTo("gauge");
    }

    @Test
    @DisplayName("realty.visit-funnel 验证 code 及 stages 结构")
    void visit_funnel_code_and_stages() {
        DashboardCardProvider p = providers.stream()
                .filter(c -> "realty.visit-funnel".equals(c.code()))
                .findFirst().orElseThrow();
        assertThat(p.metadata().suggestedChart()).isEqualTo("funnel");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) p.render(ctx);
        assertThat(data).containsKey("stages");
        Object[] stages = (Object[]) data.get("stages");
        assertThat(stages).hasSize(4);
    }

    @Test
    @DisplayName("realty.channel-roi 验证 manager/admin 限权且 rows 结构正确")
    void channel_roi_roles_and_rows() {
        DashboardCardProvider p = providers.stream()
                .filter(c -> "realty.channel-roi".equals(c.code()))
                .findFirst().orElseThrow();

        // sales 不可见
        assertThat(p.metadata().applicableRoles()).doesNotContain("sales");
        assertThat(p.metadata().applicableRoles()).containsAll(List.of("manager", "admin"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) p.render(ctx);
        assertThat(data).containsKey("rows");
    }
}
