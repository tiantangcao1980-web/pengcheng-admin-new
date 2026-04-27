package com.pengcheng.system.plugin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.plugin.entity.IndustryPluginDef;
import com.pengcheng.system.plugin.entity.TenantPlugin;
import com.pengcheng.system.plugin.mapper.IndustryPluginDefMapper;
import com.pengcheng.system.plugin.mapper.TenantPluginMapper;
import com.pengcheng.system.plugin.registry.IndustryPluginRegistry;
import com.pengcheng.system.plugin.spi.IndustryPlugin;
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
@DisplayName("IndustryPluginRegistry")
class IndustryPluginRegistryTest {

    @Mock
    private IndustryPluginDefMapper pluginDefMapper;
    @Mock
    private TenantPluginMapper tenantPluginMapper;

    // ---------------------------------------------------------------- 用例 1

    @Test
    @DisplayName("注入收集：多个 IndustryPlugin Bean 都能被收集到 registry")
    void should_collect_all_plugins() {
        IndustryPlugin p1 = stubPlugin("realty", "房产销售");
        IndustryPlugin p2 = stubPlugin("edu", "教育培训");
        IndustryPlugin p3 = stubPlugin("decoration", "家装设计");

        IndustryPluginRegistry registry = new IndustryPluginRegistry(
                List.of(p1, p2, p3), pluginDefMapper, tenantPluginMapper);

        assertThat(registry.listAll()).hasSize(3);
        assertThat(registry.listAll().stream().map(IndustryPlugin::code))
                .containsExactlyInAnyOrder("realty", "edu", "decoration");
    }

    // ---------------------------------------------------------------- 用例 2

    @Test
    @DisplayName("findByCode：存在返回 present，不存在返回 empty")
    void should_find_plugin_by_code() {
        IndustryPlugin realty = stubPlugin("realty", "房产销售");

        IndustryPluginRegistry registry = new IndustryPluginRegistry(
                List.of(realty), pluginDefMapper, tenantPluginMapper);

        assertThat(registry.findByCode("realty")).isPresent();
        assertThat(registry.findByCode("edu")).isEmpty();
    }

    // ---------------------------------------------------------------- 用例 3

    @Test
    @DisplayName("syncToDb 幂等：首次 INSERT，再次调用 UPDATE（不触发第二次 INSERT）")
    void should_sync_idempotent() {
        IndustryPlugin realty = stubPlugin("realty", "房产销售");
        IndustryPluginRegistry registry = new IndustryPluginRegistry(
                List.of(realty), pluginDefMapper, tenantPluginMapper);

        // 第一次同步：不存在 → INSERT
        when(pluginDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        registry.syncToDb();
        verify(pluginDefMapper, times(1)).insert(any(IndustryPluginDef.class));

        // 第二次同步：已存在 → UPDATE
        reset(pluginDefMapper);
        IndustryPluginDef existing = new IndustryPluginDef();
        existing.setId(1L);
        existing.setCode("realty");
        existing.setEnabled(0);
        when(pluginDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        registry.syncToDb();
        verify(pluginDefMapper, times(0)).insert(any());
        verify(pluginDefMapper, times(1)).updateById(any());
    }

    // ---------------------------------------------------------------- 用例 4

    @Test
    @DisplayName("syncToDb 不覆盖 enabled：管理员手动设置 enabled=1，更新后 enabled 仍为 1")
    void should_not_overwrite_enabled_on_sync() {
        IndustryPlugin realty = stubPlugin("realty", "房产销售");
        IndustryPluginRegistry registry = new IndustryPluginRegistry(
                List.of(realty), pluginDefMapper, tenantPluginMapper);

        IndustryPluginDef existing = new IndustryPluginDef();
        existing.setId(1L);
        existing.setCode("realty");
        existing.setEnabled(1); // 管理员手动开启
        when(pluginDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        registry.syncToDb();

        ArgumentCaptor<IndustryPluginDef> captor = ArgumentCaptor.forClass(IndustryPluginDef.class);
        verify(pluginDefMapper).updateById(captor.capture());
        // enabled 字段应保持不变（仍为 1，未被覆盖为 0）
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- 用例 5

    @Test
    @DisplayName("单插件同步异常隔离：一个插件失败不影响其他插件完成同步")
    void should_isolate_single_plugin_failure() {
        IndustryPlugin bad = stubPlugin("bad", "异常插件");
        IndustryPlugin good = stubPlugin("edu", "教育培训");
        IndustryPluginRegistry registry = new IndustryPluginRegistry(
                List.of(bad, good), pluginDefMapper, tenantPluginMapper);

        // bad 插件查询时抛异常
        when(pluginDefMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(null); // good 插件正常

        // 不应抛出异常
        registry.syncToDb();

        // good 插件应该完成 INSERT
        verify(pluginDefMapper, atLeastOnce()).insert(argThat(
                def -> "edu".equals(def.getCode())));
    }

    // ---------------------------------------------------------------- 工具方法

    private IndustryPlugin stubPlugin(String code, String name) {
        IndustryPlugin plugin = mock(IndustryPlugin.class);
        when(plugin.code()).thenReturn(code);
        IndustryPlugin.PluginMetadata meta = mock(IndustryPlugin.PluginMetadata.class);
        when(meta.name()).thenReturn(name);
        when(meta.version()).thenReturn("1.0.0");
        when(meta.description()).thenReturn(name + " 描述");
        when(meta.vendor()).thenReturn("MasterLife");
        when(meta.icon()).thenReturn("TestIcon");
        when(plugin.metadata()).thenReturn(meta);
        return plugin;
    }
}
