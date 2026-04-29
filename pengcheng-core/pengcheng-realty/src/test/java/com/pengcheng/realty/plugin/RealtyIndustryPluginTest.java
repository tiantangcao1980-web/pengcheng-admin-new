package com.pengcheng.realty.plugin;

import com.pengcheng.system.plugin.spi.IndustryPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RealtyIndustryPlugin")
class RealtyIndustryPluginTest {

    private final RealtyIndustryPlugin plugin = new RealtyIndustryPlugin();

    // ---------------------------------------------------------------- 用例 1

    @Test
    @DisplayName("metadata 字段非空：code/name/version/description/vendor/icon 均已赋值")
    void should_have_non_null_metadata_fields() {
        assertThat(plugin.code()).isNotBlank();
        assertThat(plugin.code()).isEqualTo("realty");

        IndustryPlugin.PluginMetadata meta = plugin.metadata();
        assertThat(meta).isNotNull();
        assertThat(meta.name()).isNotBlank();
        assertThat(meta.version()).isNotBlank();
        assertThat(meta.description()).isNotBlank();
        assertThat(meta.vendor()).isNotBlank();
        assertThat(meta.icon()).isNotBlank();
    }

    // ---------------------------------------------------------------- 用例 2

    @Test
    @DisplayName("menus + cards + field templates 均非空，且字段完整")
    void should_have_non_empty_contributions() {
        // 菜单贡献
        assertThat(plugin.contributeMenus()).isNotEmpty();
        plugin.contributeMenus().forEach(mc -> {
            assertThat(mc.path()).isNotBlank();
            assertThat(mc.name()).isNotBlank();
            assertThat(mc.parent()).isNotNull(); // parent 可为空串（顶级），但不能 null
            assertThat(mc.component()).isNotNull();
            assertThat(mc.permission()).isNotBlank();
            assertThat(mc.icon()).isNotBlank();
        });

        // 看板卡片 code
        assertThat(plugin.contributeDashboardCardCodes()).isNotEmpty();
        plugin.contributeDashboardCardCodes().forEach(code ->
                assertThat(code).startsWith("realty."));

        // 字段模板
        assertThat(plugin.contributeFieldTemplates()).isNotEmpty();
        plugin.contributeFieldTemplates().forEach(tpl -> {
            assertThat(tpl.entityType()).isNotBlank();
            assertThat(tpl.templateName()).isNotBlank();
            assertThat(tpl.fields()).isNotEmpty();
            tpl.fields().forEach(f -> {
                assertThat(f.key()).isNotBlank();
                assertThat(f.label()).isNotBlank();
                assertThat(f.fieldType()).isNotBlank();
            });
        });
    }
}
