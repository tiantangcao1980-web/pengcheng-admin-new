package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.plugin.dto.PluginVO;
import com.pengcheng.system.plugin.registry.IndustryPluginRegistry;
import com.pengcheng.system.plugin.service.IndustryPluginService;
import com.pengcheng.system.plugin.spi.IndustryPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行业插件管理接口。
 *
 * <pre>
 * GET  /admin/plugins                            列出全部插件（含当前租户启用状态）
 * POST /admin/plugins/{code}/enable?tenantId=   为指定租户启用插件
 * POST /admin/plugins/{code}/disable?tenantId=  为指定租户禁用插件
 * GET  /admin/plugins/{code}/menus              查看插件菜单贡献
 * GET  /admin/plugins/{code}/field-templates    查看插件字段模板贡献
 * </pre>
 */
@RestController
@RequestMapping("/admin/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final IndustryPluginService pluginService;
    private final IndustryPluginRegistry pluginRegistry;

    /**
     * 列出全部插件及指定租户的启用状态。
     *
     * @param tenantId 租户ID（可选，不传则 tenantEnabled 均为 false）
     */
    @GetMapping
    @SaCheckPermission("system:plugin:manage")
    public Result<List<PluginVO>> listPlugins(
            @RequestParam(required = false) Long tenantId) {
        return Result.ok(pluginService.listAvailable(tenantId));
    }

    /**
     * 为指定租户启用插件。
     *
     * @param code     插件 code
     * @param tenantId 目标租户ID
     */
    @PostMapping("/{code}/enable")
    @SaCheckPermission("system:plugin:manage")
    public Result<Void> enable(
            @PathVariable String code,
            @RequestParam Long tenantId) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        pluginService.enableForTenant(tenantId, code, operatorId);
        return Result.ok();
    }

    /**
     * 为指定租户禁用插件。
     *
     * @param code     插件 code
     * @param tenantId 目标租户ID
     */
    @PostMapping("/{code}/disable")
    @SaCheckPermission("system:plugin:manage")
    public Result<Void> disable(
            @PathVariable String code,
            @RequestParam Long tenantId) {
        pluginService.disableForTenant(tenantId, code);
        return Result.ok();
    }

    /**
     * 查看插件的菜单贡献列表（不依赖租户上下文，展示插件静态声明的全部菜单）。
     *
     * @param code 插件 code
     */
    @GetMapping("/{code}/menus")
    @SaCheckPermission("system:plugin:manage")
    public Result<List<IndustryPlugin.MenuContribution>> getMenus(@PathVariable String code) {
        return pluginRegistry.findByCode(code)
                .map(p -> Result.ok(p.contributeMenus()))
                .orElse(Result.fail("插件不存在：" + code));
    }

    /**
     * 查看插件的自定义字段模板贡献。
     *
     * @param code 插件 code
     */
    @GetMapping("/{code}/field-templates")
    @SaCheckPermission("system:plugin:manage")
    public Result<List<IndustryPlugin.FieldTemplateContribution>> getFieldTemplates(
            @PathVariable String code) {
        return pluginRegistry.findByCode(code)
                .map(p -> Result.ok(p.contributeFieldTemplates()))
                .orElse(Result.fail("插件不存在：" + code));
    }
}
