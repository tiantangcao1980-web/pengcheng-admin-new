package com.pengcheng.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Integration Provider 配置管理 Service（管理员 CRUD）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConfigService {

    private final IntegrationProviderConfigMapper configMapper;

    /**
     * 查询租户下所有 provider 配置。
     */
    public List<IntegrationProviderConfig> listByTenant(Long tenantId) {
        return configMapper.selectList(
                new LambdaQueryWrapper<IntegrationProviderConfig>()
                        .eq(IntegrationProviderConfig::getTenantId, tenantId)
                        .orderByAsc(IntegrationProviderConfig::getProvider));
    }

    /**
     * 查询单条配置。
     */
    public IntegrationProviderConfig getByTenantAndProvider(Long tenantId, String provider) {
        return configMapper.selectOne(
                new LambdaQueryWrapper<IntegrationProviderConfig>()
                        .eq(IntegrationProviderConfig::getTenantId, tenantId)
                        .eq(IntegrationProviderConfig::getProvider, provider));
    }

    /**
     * 新增或更新 provider 配置（按 tenant_id + provider 做 upsert）。
     */
    public IntegrationProviderConfig saveOrUpdate(IntegrationProviderConfig config) {
        IntegrationProviderConfig existing =
                getByTenantAndProvider(config.getTenantId(), config.getProvider());
        if (existing == null) {
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
            return config;
        } else {
            config.setId(existing.getId());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
            return config;
        }
    }

    /**
     * 删除配置。
     */
    public void delete(Long id) {
        configMapper.deleteById(id);
    }

    /**
     * 更新最后同步状态。
     */
    public void updateSyncStatus(Long tenantId, String provider, String status) {
        IntegrationProviderConfig cfg = getByTenantAndProvider(tenantId, provider);
        if (cfg == null) return;
        cfg.setLastSyncTime(LocalDateTime.now());
        cfg.setLastSyncStatus(status);
        cfg.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(cfg);
    }
}
