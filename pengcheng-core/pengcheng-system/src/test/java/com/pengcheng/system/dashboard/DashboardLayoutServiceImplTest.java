package com.pengcheng.system.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import com.pengcheng.system.dashboard.entity.DashboardLayout;
import com.pengcheng.system.dashboard.mapper.DashboardCardDefMapper;
import com.pengcheng.system.dashboard.mapper.DashboardLayoutMapper;
import com.pengcheng.system.dashboard.service.impl.DashboardLayoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardLayoutServiceImpl")
class DashboardLayoutServiceImplTest {

    @Mock
    private DashboardLayoutMapper layoutMapper;

    @Mock
    private DashboardCardDefMapper cardDefMapper;

    private DashboardLayoutServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardLayoutServiceImpl(layoutMapper, cardDefMapper);
    }

    @Test
    @DisplayName("getDefault fallback：数据库无默认布局时自动生成内置布局（含前 6 个卡片）")
    void should_fallback_to_builtin_layout_when_no_default() {
        when(layoutMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // 模拟 3 个已启用卡片
        DashboardCardDef c1 = cardDef(1L, "sales.funnel", 4, 3);
        DashboardCardDef c2 = cardDef(2L, "customer.health", 6, 4);
        DashboardCardDef c3 = cardDef(3L, "team.workload", 4, 3);
        when(cardDefMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2, c3));

        DashboardLayout layout = service.getDefault("USER", 42L);

        assertThat(layout).isNotNull();
        assertThat(layout.getOwnerType()).isEqualTo("USER");
        assertThat(layout.getOwnerId()).isEqualTo(42L);
        assertThat(layout.getIsDefault()).isEqualTo(1);
        // layoutJson 应包含所有 3 个卡片
        String json = layout.getLayoutJson();
        assertThat(json).contains("sales.funnel");
        assertThat(json).contains("customer.health");
        assertThat(json).contains("team.workload");
        // fallback 不应写库
        verify(layoutMapper, never()).insert(any());
    }

    @Test
    @DisplayName("saveLayout 设为默认时，先清除同 owner 其他默认标记，再保存")
    void should_clear_other_defaults_before_saving_new_default() {
        DashboardLayout layout = new DashboardLayout();
        layout.setOwnerType("USER");
        layout.setOwnerId(10L);
        layout.setName("我的布局");
        layout.setLayoutJson("[{\"cardCode\":\"sales.funnel\",\"x\":0,\"y\":0,\"w\":4,\"h\":3}]");
        layout.setIsDefault(1);

        service.saveLayout(layout);

        // 应先调 update 清除旧 default
        verify(layoutMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        // 再 insert（id 为 null）
        verify(layoutMapper, times(1)).insert(layout);
        verify(layoutMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("listByOwner 返回该 owner 的所有布局列表")
    void should_list_layouts_by_owner() {
        DashboardLayout l1 = new DashboardLayout();
        l1.setId(1L); l1.setOwnerType("USER"); l1.setOwnerId(5L); l1.setIsDefault(1);
        DashboardLayout l2 = new DashboardLayout();
        l2.setId(2L); l2.setOwnerType("USER"); l2.setOwnerId(5L); l2.setIsDefault(0);
        when(layoutMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(l1, l2));

        List<DashboardLayout> result = service.listByOwner("USER", 5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    // ---------------------------------------------------------------- 工具方法

    private DashboardCardDef cardDef(Long id, String code, int cols, int rows) {
        DashboardCardDef def = new DashboardCardDef();
        def.setId(id);
        def.setCode(code);
        def.setEnabled(1);
        def.setDefaultCols(cols);
        def.setDefaultRows(rows);
        return def;
    }
}
