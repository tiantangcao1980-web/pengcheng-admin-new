package com.pengcheng.system.dashboard.registry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import com.pengcheng.system.dashboard.mapper.DashboardCardDefMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 看板卡片注册中心。
 *
 * <p>启动时自动收集所有 {@link DashboardCardProvider} Bean，并将 SPI 元数据
 * 同步至 {@code dashboard_card_def} 表（INSERT ON DUPLICATE KEY UPDATE 语义）。
 *
 * <h3>syncToDb 策略</h3>
 * 遍历所有已注册的 provider，对每条元数据执行：
 * <ol>
 *   <li>按 code 查询是否已存在记录；</li>
 *   <li>不存在 → INSERT（enabled 默认 1）；</li>
 *   <li>已存在 → UPDATE name/category/suggestedChart/defaultCols/defaultRows/description，
 *       <b>不覆盖 enabled</b>（保留管理员手动设置值）。</li>
 * </ol>
 * 该方法在 {@code ContextRefreshedEvent} 触发后执行，幂等安全。
 */
@Slf4j
@Component
public class DashboardCardRegistry implements ApplicationListener<ContextRefreshedEvent> {

    /** 已注册的 provider 映射（code → provider）*/
    private final Map<String, DashboardCardProvider> providerMap = new ConcurrentHashMap<>();

    private final DashboardCardDefMapper cardDefMapper;

    /**
     * Spring 自动注入所有实现了 {@link DashboardCardProvider} 的 Bean。
     * 当容器中没有任何实现时，{@code providers} 为空列表，不抛异常。
     */
    public DashboardCardRegistry(List<DashboardCardProvider> providers,
                                  DashboardCardDefMapper cardDefMapper) {
        this.cardDefMapper = cardDefMapper;
        if (providers != null) {
            for (DashboardCardProvider p : providers) {
                providerMap.put(p.code(), p);
            }
        }
        log.info("[DashboardCardRegistry] 已注册 {} 个卡片 provider: {}",
                providerMap.size(), providerMap.keySet());
    }

    // ---------------------------------------------------------------- 查询 API

    /**
     * 根据 code 查找 provider；不存在返回 {@code Optional.empty()}。
     */
    public Optional<DashboardCardProvider> findByCode(String code) {
        return Optional.ofNullable(providerMap.get(code));
    }

    /**
     * 返回所有已注册 provider 的不可变列表。
     */
    public List<DashboardCardProvider> listAll() {
        return Collections.unmodifiableList(new ArrayList<>(providerMap.values()));
    }

    /**
     * 按 category 过滤 provider 列表（大小写不敏感）。
     */
    public List<DashboardCardProvider> listByCategory(String category) {
        return providerMap.values().stream()
                .filter(p -> category.equalsIgnoreCase(p.metadata().category()))
                .collect(Collectors.toList());
    }

    /**
     * 返回对给定角色集合可见的 provider 列表。
     *
     * <p>规则：{@code metadata.applicableRoles()} 为空集 → 所有角色可见；
     * 否则取交集非空才可见。
     */
    public List<DashboardCardProvider> applicableForRoles(Set<String> roles) {
        return providerMap.values().stream()
                .filter(p -> {
                    Set<String> applicable = p.metadata().applicableRoles();
                    if (applicable == null || applicable.isEmpty()) {
                        return true;
                    }
                    return roles != null && roles.stream().anyMatch(applicable::contains);
                })
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
     * 将所有 SPI 元数据同步到 {@code dashboard_card_def} 表，幂等。
     */
    public void syncToDb() {
        if (providerMap.isEmpty()) {
            log.debug("[DashboardCardRegistry] 无 provider 需要同步");
            return;
        }
        int inserted = 0;
        int updated = 0;
        for (DashboardCardProvider p : providerMap.values()) {
            try {
                DashboardCardProvider.DashboardCardMetadata meta = p.metadata();
                DashboardCardDef existing = cardDefMapper.selectOne(
                        new LambdaQueryWrapper<DashboardCardDef>()
                                .eq(DashboardCardDef::getCode, p.code())
                );
                if (existing == null) {
                    DashboardCardDef def = buildDef(p.code(), meta);
                    cardDefMapper.insert(def);
                    inserted++;
                } else {
                    // 仅更新展示字段，保留 enabled 值
                    existing.setName(meta.name());
                    existing.setCategory(meta.category());
                    existing.setSuggestedChart(meta.suggestedChart());
                    existing.setDefaultCols(meta.defaultCols());
                    existing.setDefaultRows(meta.defaultRows());
                    existing.setDescription(meta.description());
                    cardDefMapper.updateById(existing);
                    updated++;
                }
            } catch (Exception e) {
                log.warn("[DashboardCardRegistry] 同步卡片 [{}] 失败，已跳过: {}", p.code(), e.getMessage());
            }
        }
        log.info("[DashboardCardRegistry] syncToDb 完成，新增 {} 条，更新 {} 条", inserted, updated);
    }

    private DashboardCardDef buildDef(String code, DashboardCardProvider.DashboardCardMetadata meta) {
        DashboardCardDef def = new DashboardCardDef();
        def.setCode(code);
        def.setName(meta.name());
        def.setCategory(meta.category());
        def.setSuggestedChart(meta.suggestedChart());
        def.setDefaultCols(meta.defaultCols());
        def.setDefaultRows(meta.defaultRows());
        def.setDescription(meta.description());
        def.setEnabled(1);
        return def;
    }
}
