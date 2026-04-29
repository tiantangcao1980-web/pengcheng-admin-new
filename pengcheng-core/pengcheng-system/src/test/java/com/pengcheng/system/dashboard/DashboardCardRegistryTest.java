package com.pengcheng.system.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import com.pengcheng.system.dashboard.mapper.DashboardCardDefMapper;
import com.pengcheng.system.dashboard.registry.DashboardCardRegistry;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardCardRegistry")
class DashboardCardRegistryTest {

    @Mock
    private DashboardCardDefMapper cardDefMapper;

    private DashboardCardProvider salesProvider;
    private DashboardCardProvider customerProvider;
    private DashboardCardProvider teamProvider;

    @BeforeEach
    void setUp() {
        salesProvider = mockProvider("sales.funnel", "销售漏斗", "sales",
                Set.of("admin", "sales"), "funnel", 4, 3);
        customerProvider = mockProvider("customer.health", "客户健康度", "customer",
                Set.of("admin", "manager"), "bar", 6, 4);
        teamProvider = mockProvider("team.workload", "团队工作量", "team",
                Collections.emptySet(), "line", 4, 3);
    }

    @Test
    @DisplayName("registry 收集多个 provider，listAll 返回全部")
    void should_collect_all_providers() {
        DashboardCardRegistry registry = new DashboardCardRegistry(
                List.of(salesProvider, customerProvider, teamProvider), cardDefMapper);

        List<DashboardCardProvider> all = registry.listAll();
        assertThat(all).hasSize(3);
        assertThat(all.stream().map(DashboardCardProvider::code))
                .containsExactlyInAnyOrder("sales.funnel", "customer.health", "team.workload");
    }

    @Test
    @DisplayName("findByCode 找到存在的 provider")
    void should_find_provider_by_code() {
        DashboardCardRegistry registry = new DashboardCardRegistry(
                List.of(salesProvider, customerProvider), cardDefMapper);

        Optional<DashboardCardProvider> result = registry.findByCode("sales.funnel");
        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo("sales.funnel");

        Optional<DashboardCardProvider> notFound = registry.findByCode("non.exist");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("listByCategory 按分类过滤")
    void should_filter_by_category() {
        DashboardCardRegistry registry = new DashboardCardRegistry(
                List.of(salesProvider, customerProvider, teamProvider), cardDefMapper);

        List<DashboardCardProvider> salesCards = registry.listByCategory("sales");
        assertThat(salesCards).hasSize(1);
        assertThat(salesCards.get(0).code()).isEqualTo("sales.funnel");

        List<DashboardCardProvider> unknownCat = registry.listByCategory("finance");
        assertThat(unknownCat).isEmpty();
    }

    @Test
    @DisplayName("applicableForRoles 角色过滤：空 roles 的卡片对所有人可见")
    void should_apply_role_filter_correctly() {
        DashboardCardRegistry registry = new DashboardCardRegistry(
                List.of(salesProvider, customerProvider, teamProvider), cardDefMapper);

        // sales 角色：可见 sales.funnel（sales in roles）+ team.workload（empty roles）
        Set<String> salesRoles = Set.of("sales");
        List<DashboardCardProvider> forSales = registry.applicableForRoles(salesRoles);
        assertThat(forSales.stream().map(DashboardCardProvider::code))
                .containsExactlyInAnyOrder("sales.funnel", "team.workload");

        // manager 角色：可见 customer.health + team.workload
        Set<String> managerRoles = Set.of("manager");
        List<DashboardCardProvider> forManager = registry.applicableForRoles(managerRoles);
        assertThat(forManager.stream().map(DashboardCardProvider::code))
                .containsExactlyInAnyOrder("customer.health", "team.workload");
    }

    @Test
    @DisplayName("syncToDb 幂等：已存在则更新，不存在则插入，enabled 值不被覆盖")
    void should_sync_to_db_idempotent() {
        // 模拟 sales.funnel 已存在（enabled=0，手动禁用）
        DashboardCardDef existing = new DashboardCardDef();
        existing.setId(1L);
        existing.setCode("sales.funnel");
        existing.setEnabled(0); // 手动禁用

        // customer.health 不存在
        when(cardDefMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    // 根据调用参数区分两个 code 查询，简化处理：第一次返回 existing，第二次 null
                    return null; // 统一返回 null，让两者都走 insert 分支（幂等测试）
                });

        DashboardCardRegistry registry = new DashboardCardRegistry(
                List.of(salesProvider, customerProvider), cardDefMapper);

        // 先重置 mock 以便精确控制
        reset(cardDefMapper);
        // sales.funnel -> 已存在
        when(cardDefMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existing)  // first call: sales.funnel
                .thenReturn(null);     // second call: customer.health

        registry.syncToDb();

        // sales.funnel → updateById（保留 enabled=0）
        ArgumentCaptor<DashboardCardDef> updateCaptor = ArgumentCaptor.forClass(DashboardCardDef.class);
        verify(cardDefMapper, times(1)).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getEnabled()).isEqualTo(0); // 未被覆盖

        // customer.health → insert
        verify(cardDefMapper, times(1)).insert(any(DashboardCardDef.class));
    }

    // ---------------------------------------------------------------- 工具方法

    private DashboardCardProvider mockProvider(String code, String name, String category,
                                                Set<String> roles, String chart,
                                                int cols, int rows) {
        DashboardCardProvider.DashboardCardMetadata meta =
                new DashboardCardProvider.DashboardCardMetadata() {
                    @Override public String name()           { return name; }
                    @Override public String category()       { return category; }
                    @Override public Set<String> applicableRoles() { return roles; }
                    @Override public int defaultCols()       { return cols; }
                    @Override public int defaultRows()       { return rows; }
                    @Override public String suggestedChart() { return chart; }
                    @Override public String description()    { return name + " 描述"; }
                };
        DashboardCardProvider p = mock(DashboardCardProvider.class);
        when(p.code()).thenReturn(code);
        when(p.metadata()).thenReturn(meta);
        return p;
    }
}
