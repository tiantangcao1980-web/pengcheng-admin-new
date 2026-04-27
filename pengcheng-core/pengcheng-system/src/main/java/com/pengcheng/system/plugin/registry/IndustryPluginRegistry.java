package com.pengcheng.system.plugin.registry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.plugin.entity.IndustryPluginDef;
import com.pengcheng.system.plugin.entity.TenantPlugin;
import com.pengcheng.system.plugin.mapper.IndustryPluginDefMapper;
import com.pengcheng.system.plugin.mapper.TenantPluginMapper;
import com.pengcheng.system.plugin.spi.IndustryPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 行业插件注册中心。
 *
 * <p>启动时自动收集所有 {@link IndustryPlugin} Bean，并将 SPI 元数据
 * 同步至 {@code industry_plugin} 表（幂等，不覆盖 enabled 字段）。
 *
 * <h3>syncToDb 策略</h3>
 * <ul>
 *   <li>按 code 查询是否已存在；</li>
 *   <li>不存在 → INSERT（enabled 默认 0，需管理员手动开启）；</li>
 *   <li>已存在 → UPDATE name/version/description/vendor/icon，<b>不覆盖 enabled</b>。</li>
 * </ul>
 * <p>单个插件同步异常会记录 WARN 日志并跳过，不影响其他插件。
 */
@Slf4j
@Component
public class IndustryPluginRegistry implements ApplicationListener<ContextRefreshedEvent> {

    /** code → plugin 映射 */
    private final Map<String, IndustryPlugin> pluginMap = new ConcurrentHashMap<>();

    private final IndustryPluginDefMapper pluginDefMapper;
    private final TenantPluginMapper tenantPluginMapper;

    /**
     * Spring 自动注入所有 {@link IndustryPlugin} Bean（无实现时为空列表，不抛异常）。
     */
    public IndustryPluginRegistry(List<IndustryPlugin> plugins,
                                   IndustryPluginDefMapper pluginDefMapper,
                                   TenantPluginMapper tenantPluginMapper) {
        this.pluginDefMapper = pluginDefMapper;
        this.tenantPluginMapper = tenantPluginMapper;
        if (plugins != null) {
            for (IndustryPlugin plugin : plugins) {
                pluginMap.put(plugin.code(), plugin);
            }
        }
        log.info("[IndustryPluginRegistry] 已注册 {} 个插件: {}", pluginMap.size(), pluginMap.keySet());
    }

    // ---------------------------------------------------------------- 查询 API

    /**
     * 按 code 查找插件；不存在返回 {@link Optional#empty()}。
     */
    public Optional<IndustryPlugin> findByCode(String code) {
        return Optional.ofNullable(pluginMap.get(code));
    }

    /**
     * 返回所有已注册插件的不可变列表。
     */
    public List<IndustryPlugin> listAll() {
        return Collections.unmodifiableList(new ArrayList<>(pluginMap.values()));
    }

    /**
     * 返回指定租户已启用的插件列表。
     * 规则：tenant_plugin.enabled=1 且 industry_plugin 全局 enabled=1（任一为 0 则不生效）。
     *
     * @param tenantId 租户ID
     */
    public List<IndustryPlugin> listEnabledForTenant(Long tenantId) {
        // 查询全局已启用的 code 集合
        List<IndustryPluginDef> globalEnabled = pluginDefMapper.selectList(
                new LambdaQueryWrapper<IndustryPluginDef>().eq(IndustryPluginDef::getEnabled, 1));
        Set<String> globalCodes = globalEnabled.stream()
                .map(IndustryPluginDef::getCode).collect(Collectors.toSet());

        // 查询租户已启用的 code 集合
        List<String> tenantCodes = tenantPluginMapper.selectEnabledCodes(tenantId);
        Set<String> tenantEnabledCodes = new HashSet<>(tenantCodes);

        return pluginMap.values().stream()
                .filter(p -> globalCodes.contains(p.code()) && tenantEnabledCodes.contains(p.code()))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- 生命周期

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 防止父子容器双重触发
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        syncToDb();
    }

    /**
     * 将所有 SPI 元数据同步到 {@code industry_plugin} 表，幂等。
     */
    public void syncToDb() {
        if (pluginMap.isEmpty()) {
            log.debug("[IndustryPluginRegistry] 无插件需要同步");
            return;
        }
        int inserted = 0;
        int updated = 0;
        for (IndustryPlugin plugin : pluginMap.values()) {
            try {
                IndustryPlugin.PluginMetadata meta = plugin.metadata();
                IndustryPluginDef existing = pluginDefMapper.selectOne(
                        new LambdaQueryWrapper<IndustryPluginDef>()
                                .eq(IndustryPluginDef::getCode, plugin.code()));
                if (existing == null) {
                    IndustryPluginDef def = buildDef(plugin.code(), meta);
                    pluginDefMapper.insert(def);
                    inserted++;
                } else {
                    // 更新展示字段，不覆盖 enabled（保留管理员手动设置值）
                    existing.setName(meta.name());
                    existing.setVersion(meta.version());
                    existing.setDescription(meta.description());
                    existing.setVendor(meta.vendor());
                    existing.setIcon(meta.icon());
                    pluginDefMapper.updateById(existing);
                    updated++;
                }
            } catch (Exception e) {
                log.warn("[IndustryPluginRegistry] 同步插件 [{}] 失败，已跳过: {}",
                        plugin.code(), e.getMessage());
            }
        }
        log.info("[IndustryPluginRegistry] syncToDb 完成，新增 {} 条，更新 {} 条", inserted, updated);
    }

    // ---------------------------------------------------------------- 工具方法

    private IndustryPluginDef buildDef(String code, IndustryPlugin.PluginMetadata meta) {
        IndustryPluginDef def = new IndustryPluginDef();
        def.setCode(code);
        def.setName(meta.name());
        def.setVersion(meta.version());
        def.setDescription(meta.description());
        def.setVendor(meta.vendor());
        def.setIcon(meta.icon());
        def.setEnabled(0); // 默认禁用，管理员手动开启
        return def;
    }
}
