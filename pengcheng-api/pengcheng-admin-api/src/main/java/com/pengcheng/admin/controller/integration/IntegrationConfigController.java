package com.pengcheng.admin.controller.integration;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.service.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Integration Provider 配置管理接口（管理员 CRUD）。
 */
@RestController
@RequestMapping("/admin/integration/configs")
@RequiredArgsConstructor
public class IntegrationConfigController {

    private final IntegrationConfigService configService;

    /**
     * 查询当前租户的所有 provider 配置列表。
     */
    @GetMapping
    @SaCheckPermission("integration:config:list")
    public Result<List<IntegrationProviderConfig>> list(@RequestParam Long tenantId) {
        return Result.ok(configService.listByTenant(tenantId));
    }

    /**
     * 查询单个 provider 配置详情。
     */
    @GetMapping("/{provider}")
    @SaCheckPermission("integration:config:list")
    public Result<IntegrationProviderConfig> get(@RequestParam Long tenantId,
                                                  @PathVariable String provider) {
        return Result.ok(configService.getByTenantAndProvider(tenantId, provider));
    }

    /**
     * 新增或更新 provider 配置（tenant_id + provider 唯一）。
     */
    @PostMapping
    @SaCheckPermission("integration:config:edit")
    public Result<IntegrationProviderConfig> saveOrUpdate(@RequestBody IntegrationProviderConfig config) {
        return Result.ok(configService.saveOrUpdate(config));
    }

    /**
     * 删除 provider 配置。
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("integration:config:delete")
    public Result<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return Result.ok();
    }
}
