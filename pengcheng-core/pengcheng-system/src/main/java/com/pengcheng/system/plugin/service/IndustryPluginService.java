package com.pengcheng.system.plugin.service;

import com.pengcheng.system.plugin.dto.PluginVO;
import com.pengcheng.system.plugin.spi.IndustryPlugin;

import java.util.List;

/**
 * 行业插件管理服务。
 *
 * <p>提供插件的启用/禁用、状态查询、菜单聚合等能力。
 */
public interface IndustryPluginService {

    /**
     * 为指定租户启用插件。
     *
     * <p>幂等：若已启用则不重复调用 {@link IndustryPlugin#onEnable(Long)}。
     *
     * @param tenantId  租户ID
     * @param code      插件 code
     * @param enabledBy 操作人用户ID
     */
    void enableForTenant(Long tenantId, String code, Long enabledBy);

    /**
     * 为指定租户禁用插件。
     *
     * <p>幂等：若已禁用则不重复调用 {@link IndustryPlugin#onDisable(Long)}。
     *
     * @param tenantId 租户ID
     * @param code     插件 code
     */
    void disableForTenant(Long tenantId, String code);

    /**
     * 判断某租户的指定插件是否处于启用状态（全局 enabled=1 且租户 enabled=1）。
     *
     * @param tenantId 租户ID
     * @param code     插件 code
     */
    boolean isEnabled(Long tenantId, String code);

    /**
     * 列出全部插件及当前租户的启用状态（用于插件市场展示）。
     *
     * @param tenantId 当前租户ID（可为 null，此时 tenantEnabled 均为 false）
     */
    List<PluginVO> listAvailable(Long tenantId);

    /**
     * 聚合指定租户所有已启用插件的菜单贡献，供菜单注入使用。
     *
     * @param tenantId 租户ID
     */
    List<IndustryPlugin.MenuContribution> listMenusForTenant(Long tenantId);
}
