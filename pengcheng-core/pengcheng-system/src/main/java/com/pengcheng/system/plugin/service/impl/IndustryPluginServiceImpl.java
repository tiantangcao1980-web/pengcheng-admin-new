package com.pengcheng.system.plugin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysMenu;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysRoleMenu;
import com.pengcheng.system.mapper.SysMenuMapper;
import com.pengcheng.system.mapper.SysRoleMenuMapper;
import com.pengcheng.system.plugin.dto.PluginVO;
import com.pengcheng.system.plugin.entity.IndustryPluginDef;
import com.pengcheng.system.plugin.entity.TenantPlugin;
import com.pengcheng.system.plugin.mapper.IndustryPluginDefMapper;
import com.pengcheng.system.plugin.mapper.TenantPluginMapper;
import com.pengcheng.system.plugin.registry.IndustryPluginRegistry;
import com.pengcheng.system.plugin.service.IndustryPluginService;
import com.pengcheng.system.plugin.spi.IndustryPlugin;
import com.pengcheng.system.service.SysRoleService;
import com.pengcheng.system.tenant.entity.Tenant;
import com.pengcheng.system.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 行业插件管理服务实现。
 *
 * <p>启用流程：
 * <ol>
 *   <li>写/更新 {@code tenant_plugin} 记录（enabled=1）；</li>
 *   <li>调用 {@link IndustryPlugin#onEnable(Long)} 钩子；</li>
 *   <li>将插件菜单贡献写入 {@code sys_menu} 并关联租户 admin 角色（sys_role_menu）。</li>
 * </ol>
 * 禁用流程反向执行，仅将 tenant_plugin.enabled 置 0，并调用 onDisable 钩子；
 * 不删除 sys_menu 记录（避免影响其他租户），改为将对应菜单从该租户 admin 角色解绑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryPluginServiceImpl implements IndustryPluginService {

    /** 租户管理员角色 code（与 TenantServiceImpl 保持一致） */
    private static final String TENANT_ADMIN_ROLE = "tenant_admin";

    private final IndustryPluginRegistry registry;
    private final IndustryPluginDefMapper pluginDefMapper;
    private final TenantPluginMapper tenantPluginMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleService roleService;
    private final TenantService tenantService;

    // ---------------------------------------------------------------- enable / disable

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableForTenant(Long tenantId, String code, Long enabledBy) {
        IndustryPlugin plugin = findPluginOrThrow(code);

        // 幂等检查
        TenantPlugin existing = getTenantPlugin(tenantId, code);
        if (existing != null && existing.getEnabled() == 1) {
            log.info("[Plugin] 插件 [{}] 在租户 {} 已处于启用状态，跳过", code, tenantId);
            return;
        }

        // 写 tenant_plugin
        if (existing == null) {
            TenantPlugin tp = new TenantPlugin();
            tp.setTenantId(tenantId);
            tp.setPluginCode(code);
            tp.setEnabled(1);
            tp.setEnabledBy(enabledBy);
            tp.setEnabledAt(LocalDateTime.now());
            tenantPluginMapper.insert(tp);
        } else {
            tenantPluginMapper.update(null,
                    new LambdaUpdateWrapper<TenantPlugin>()
                            .eq(TenantPlugin::getTenantId, tenantId)
                            .eq(TenantPlugin::getPluginCode, code)
                            .set(TenantPlugin::getEnabled, 1)
                            .set(TenantPlugin::getEnabledBy, enabledBy)
                            .set(TenantPlugin::getEnabledAt, LocalDateTime.now()));
        }

        // 调用钩子
        try {
            plugin.onEnable(tenantId);
        } catch (Exception e) {
            log.warn("[Plugin] onEnable 钩子 [{}] 执行失败: {}", code, e.getMessage());
        }

        // 菜单刷新：写 sys_menu + 绑定 tenant admin 角色
        refreshMenusForTenant(tenantId, plugin, true);

        log.info("[Plugin] 插件 [{}] 在租户 {} 启用完成，操作人={}", code, tenantId, enabledBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableForTenant(Long tenantId, String code) {
        IndustryPlugin plugin = findPluginOrThrow(code);

        // 幂等检查
        TenantPlugin existing = getTenantPlugin(tenantId, code);
        if (existing == null || existing.getEnabled() == 0) {
            log.info("[Plugin] 插件 [{}] 在租户 {} 已处于禁用状态，跳过", code, tenantId);
            return;
        }

        // 更新 tenant_plugin
        tenantPluginMapper.update(null,
                new LambdaUpdateWrapper<TenantPlugin>()
                        .eq(TenantPlugin::getTenantId, tenantId)
                        .eq(TenantPlugin::getPluginCode, code)
                        .set(TenantPlugin::getEnabled, 0));

        // 调用钩子
        try {
            plugin.onDisable(tenantId);
        } catch (Exception e) {
            log.warn("[Plugin] onDisable 钩子 [{}] 执行失败: {}", code, e.getMessage());
        }

        // 菜单刷新：解绑 tenant admin 角色的对应菜单
        refreshMenusForTenant(tenantId, plugin, false);

        log.info("[Plugin] 插件 [{}] 在租户 {} 禁用完成", code, tenantId);
    }

    // ---------------------------------------------------------------- 查询

    @Override
    public boolean isEnabled(Long tenantId, String code) {
        TenantPlugin tp = getTenantPlugin(tenantId, code);
        if (tp == null || tp.getEnabled() != 1) {
            return false;
        }
        // 还需全局 enabled=1
        IndustryPluginDef def = pluginDefMapper.selectOne(
                new LambdaQueryWrapper<IndustryPluginDef>()
                        .eq(IndustryPluginDef::getCode, code));
        return def != null && def.getEnabled() == 1;
    }

    @Override
    public List<PluginVO> listAvailable(Long tenantId) {
        List<IndustryPluginDef> defs = pluginDefMapper.selectList(null);
        // 当前租户已启用的 code 集合
        Set<String> tenantEnabled = (tenantId != null)
                ? tenantPluginMapper.selectEnabledCodes(tenantId).stream().collect(Collectors.toSet())
                : Set.of();

        return defs.stream().map(def -> {
            PluginVO vo = new PluginVO();
            vo.setCode(def.getCode());
            vo.setName(def.getName());
            vo.setVersion(def.getVersion());
            vo.setDescription(def.getDescription());
            vo.setVendor(def.getVendor());
            vo.setIcon(def.getIcon());
            vo.setGlobalEnabled(def.getEnabled());
            vo.setTenantEnabled(tenantEnabled.contains(def.getCode()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<IndustryPlugin.MenuContribution> listMenusForTenant(Long tenantId) {
        return registry.listEnabledForTenant(tenantId).stream()
                .flatMap(p -> p.contributeMenus().stream())
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- 内部工具

    private IndustryPlugin findPluginOrThrow(String code) {
        return registry.findByCode(code)
                .orElseThrow(() -> new BusinessException("插件不存在：" + code));
    }

    private TenantPlugin getTenantPlugin(Long tenantId, String code) {
        return tenantPluginMapper.selectOne(
                new LambdaQueryWrapper<TenantPlugin>()
                        .eq(TenantPlugin::getTenantId, tenantId)
                        .eq(TenantPlugin::getPluginCode, code));
    }

    /**
     * 将插件菜单贡献写入 sys_menu，并根据 enable 参数绑定或解绑租户 admin 角色。
     *
     * <p>菜单写入策略：按 path 查重，不存在则 INSERT，存在则跳过（保留人工修改）。
     * 角色菜单绑定：仅处理租户 admin 角色，其他角色权限由管理员自行配置。
     */
    private void refreshMenusForTenant(Long tenantId, IndustryPlugin plugin, boolean enable) {
        List<IndustryPlugin.MenuContribution> menus = plugin.contributeMenus();
        if (menus == null || menus.isEmpty()) {
            return;
        }

        // 查找租户 admin 角色
        SysRole adminRole = roleService.getByCode(TENANT_ADMIN_ROLE);
        Long roleId = adminRole != null ? adminRole.getId() : null;

        for (IndustryPlugin.MenuContribution mc : menus) {
            try {
                // 查询是否已存在该路径的菜单
                SysMenu existing = menuMapper.selectOne(
                        new LambdaQueryWrapper<SysMenu>()
                                .eq(SysMenu::getPath, mc.path())
                                .eq(SysMenu::getDeleted, 0));

                Long menuId;
                if (existing == null) {
                    if (!enable) {
                        // 禁用时菜单不存在则无需处理
                        continue;
                    }
                    // 插入新菜单
                    SysMenu menu = buildMenu(mc);
                    menuMapper.insert(menu);
                    menuId = menu.getId();
                } else {
                    menuId = existing.getId();
                }

                if (roleId == null) {
                    continue;
                }

                if (enable) {
                    // 绑定 admin 角色：幂等
                    SysRoleMenu existingRm = roleMenuMapper.selectOne(
                            new LambdaQueryWrapper<SysRoleMenu>()
                                    .eq(SysRoleMenu::getRoleId, roleId)
                                    .eq(SysRoleMenu::getMenuId, menuId));
                    if (existingRm == null) {
                        SysRoleMenu rm = new SysRoleMenu();
                        rm.setRoleId(roleId);
                        rm.setMenuId(menuId);
                        roleMenuMapper.insert(rm);
                    }
                } else {
                    // 解绑 admin 角色
                    roleMenuMapper.delete(
                            new LambdaQueryWrapper<SysRoleMenu>()
                                    .eq(SysRoleMenu::getRoleId, roleId)
                                    .eq(SysRoleMenu::getMenuId, menuId));
                }
            } catch (Exception e) {
                log.warn("[Plugin] 刷新菜单 [{}] 失败，已跳过: {}", mc.path(), e.getMessage());
            }
        }
    }

    private SysMenu buildMenu(IndustryPlugin.MenuContribution mc) {
        SysMenu menu = new SysMenu();
        menu.setPath(mc.path());
        menu.setName(mc.name());
        menu.setComponent(mc.component());
        menu.setPermission(mc.permission());
        menu.setIcon(mc.icon());
        menu.setSort(mc.sort());
        menu.setType(2);    // 菜单类型
        menu.setVisible(1); // 显示
        menu.setStatus(1);  // 启用
        menu.setIsFrame(0); // 非外链
        // 父菜单通过 path 反查（简化实现：顶级菜单 parentId=0）
        if (mc.parent() == null || mc.parent().isBlank()) {
            menu.setParentId(0L);
        } else {
            SysMenu parentMenu = menuMapper.selectOne(
                    new LambdaQueryWrapper<SysMenu>()
                            .eq(SysMenu::getPath, mc.parent())
                            .eq(SysMenu::getDeleted, 0));
            menu.setParentId(parentMenu != null ? parentMenu.getId() : 0L);
        }
        return menu;
    }
}
