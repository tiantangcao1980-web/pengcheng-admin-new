package com.pengcheng.system.plugin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.plugin.entity.IndustryPluginDef;
import com.pengcheng.system.plugin.entity.TenantPlugin;
import com.pengcheng.system.plugin.mapper.IndustryPluginDefMapper;
import com.pengcheng.system.plugin.mapper.TenantPluginMapper;
import com.pengcheng.system.plugin.registry.IndustryPluginRegistry;
import com.pengcheng.system.plugin.service.impl.IndustryPluginServiceImpl;
import com.pengcheng.system.plugin.spi.IndustryPlugin;
import com.pengcheng.system.entity.SysMenu;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.mapper.SysMenuMapper;
import com.pengcheng.system.mapper.SysRoleMenuMapper;
import com.pengcheng.system.plugin.dto.PluginVO;
import com.pengcheng.system.service.SysRoleService;
import com.pengcheng.system.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndustryPluginServiceImpl")
class IndustryPluginServiceImplTest {

    @Mock private IndustryPluginRegistry registry;
    @Mock private IndustryPluginDefMapper pluginDefMapper;
    @Mock private TenantPluginMapper tenantPluginMapper;
    @Mock private SysMenuMapper menuMapper;
    @Mock private SysRoleMenuMapper roleMenuMapper;
    @Mock private SysRoleService roleService;
    @Mock private TenantService tenantService;

    private IndustryPluginServiceImpl service;
    private IndustryPlugin mockPlugin;

    @BeforeEach
    void setUp() {
        service = new IndustryPluginServiceImpl(
                registry, pluginDefMapper, tenantPluginMapper,
                menuMapper, roleMenuMapper, roleService, tenantService);
        mockPlugin = buildPlugin("realty");
    }

    // ---------------------------------------------------------------- 用例 1

    @Test
    @DisplayName("enable 调用 onEnable：enableForTenant 后 plugin.onEnable 被触发")
    void should_call_onEnable_when_enabled() {
        when(registry.findByCode("realty")).thenReturn(java.util.Optional.of(mockPlugin));
        when(tenantPluginMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(mockPlugin.contributeMenus()).thenReturn(List.of());
        when(roleService.getByCode(any())).thenReturn(null);

        service.enableForTenant(1L, "realty", 100L);

        verify(mockPlugin).onEnable(1L);
    }

    // ---------------------------------------------------------------- 用例 2

    @Test
    @DisplayName("disable 调用 onDisable：disableForTenant 后 plugin.onDisable 被触发")
    void should_call_onDisable_when_disabled() {
        when(registry.findByCode("realty")).thenReturn(java.util.Optional.of(mockPlugin));
        TenantPlugin tp = new TenantPlugin();
        tp.setEnabled(1);
        when(tenantPluginMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tp);
        when(mockPlugin.contributeMenus()).thenReturn(List.of());
        when(roleService.getByCode(any())).thenReturn(null);

        service.disableForTenant(1L, "realty");

        verify(mockPlugin).onDisable(1L);
    }

    // ---------------------------------------------------------------- 用例 3

    @Test
    @DisplayName("listAvailable 含状态：返回所有插件列表，且包含租户启用状态字段")
    void should_list_available_with_tenant_status() {
        IndustryPluginDef def = new IndustryPluginDef();
        def.setCode("realty");
        def.setName("房产销售");
        def.setVersion("1.0.0");
        def.setDescription("描述");
        def.setVendor("MasterLife");
        def.setIcon("Icon");
        def.setEnabled(1);
        when(pluginDefMapper.selectList(null)).thenReturn(List.of(def));
        when(tenantPluginMapper.selectEnabledCodes(1L)).thenReturn(List.of("realty"));

        List<PluginVO> result = service.listAvailable(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("realty");
        assertThat(result.get(0).getTenantEnabled()).isTrue();
    }

    // ---------------------------------------------------------------- 用例 4

    @Test
    @DisplayName("isEnabled：全局 enabled=1 且租户 enabled=1 时返回 true，任一为 0 则 false")
    void should_check_isEnabled_correctly() {
        // Case A: 全局 enabled=0 → false
        TenantPlugin tp = new TenantPlugin();
        tp.setEnabled(1);
        when(tenantPluginMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tp);
        IndustryPluginDef def = new IndustryPluginDef();
        def.setEnabled(0); // 全局禁用
        when(pluginDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(def);
        assertThat(service.isEnabled(1L, "realty")).isFalse();

        // Case B: 全局 enabled=1 且租户 enabled=1 → true
        reset(tenantPluginMapper, pluginDefMapper);
        TenantPlugin tp2 = new TenantPlugin();
        tp2.setEnabled(1);
        when(tenantPluginMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tp2);
        IndustryPluginDef def2 = new IndustryPluginDef();
        def2.setEnabled(1);
        when(pluginDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(def2);
        assertThat(service.isEnabled(1L, "realty")).isTrue();
    }

    // ---------------------------------------------------------------- 用例 5

    @Test
    @DisplayName("重复 enable 幂等：已启用时不再调用 onEnable")
    void should_be_idempotent_on_double_enable() {
        when(registry.findByCode("realty")).thenReturn(java.util.Optional.of(mockPlugin));
        TenantPlugin tp = new TenantPlugin();
        tp.setEnabled(1); // 已启用
        when(tenantPluginMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tp);

        service.enableForTenant(1L, "realty", 100L);

        // onEnable 不应被调用
        verify(mockPlugin, never()).onEnable(any());
    }

    // ---------------------------------------------------------------- 工具方法

    private IndustryPlugin buildPlugin(String code) {
        IndustryPlugin p = mock(IndustryPlugin.class);
        when(p.code()).thenReturn(code);
        IndustryPlugin.PluginMetadata meta = mock(IndustryPlugin.PluginMetadata.class);
        when(meta.name()).thenReturn("房产销售");
        when(p.metadata()).thenReturn(meta);
        return p;
    }
}
