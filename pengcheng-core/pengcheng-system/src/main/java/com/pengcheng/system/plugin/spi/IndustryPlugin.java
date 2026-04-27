package com.pengcheng.system.plugin.spi;

import java.util.List;

/**
 * 行业插件 SPI。
 *
 * <p>每个行业插件（房产、教培、家装等）实现此接口并标注 {@code @Component}，
 * Spring 容器启动后会被 {@link com.pengcheng.system.plugin.registry.IndustryPluginRegistry}
 * 自动收集并同步元数据到 {@code industry_plugin} 表。
 *
 * <p>贡献点说明：
 * <ul>
 *   <li>{@link #contributeMenus()} — 返回该插件注入的菜单项，启用时由 Service 写入 sys_menu</li>
 *   <li>{@link #contributeDashboardCardCodes()} — 声明该插件关联的看板卡片 code（由 K4 实现具体卡片）</li>
 *   <li>{@link #contributeFieldTemplates()} — 声明该插件的自定义字段模板（复用 V4 自定义字段机制）</li>
 *   <li>{@link #onEnable(Long)} / {@link #onDisable(Long)} — 启用/禁用钩子，默认 noop</li>
 * </ul>
 */
public interface IndustryPlugin {

    /**
     * 插件唯一编码，全局不重复，e.g. {@code "realty"}, {@code "edu"}, {@code "decoration"}。
     */
    String code();

    /**
     * 插件元数据（名称、版本、描述、提供方、图标）。
     */
    PluginMetadata metadata();

    /**
     * 菜单贡献列表。启用该插件时，由 Service 将这些菜单写入 sys_menu 并关联租户 admin 角色。
     */
    List<MenuContribution> contributeMenus();

    /**
     * 看板卡片 code 列表。启用后这些卡片对该租户可见（卡片实现由 K4 完成）。
     */
    List<String> contributeDashboardCardCodes();

    /**
     * 自定义字段模板贡献。每个模板绑定到特定实体类型，提供预置字段定义。
     */
    List<FieldTemplateContribution> contributeFieldTemplates();

    /**
     * 插件启用钩子（默认 noop）。可在此处执行初始化逻辑，如写入默认配置、初始化数据。
     *
     * @param tenantId 被启用的租户ID
     */
    default void onEnable(Long tenantId) {
    }

    /**
     * 插件禁用钩子（默认 noop）。可在此处执行清理逻辑。
     *
     * @param tenantId 被禁用的租户ID
     */
    default void onDisable(Long tenantId) {
    }

    // ================================================================
    // 内嵌类型定义
    // ================================================================

    /**
     * 插件元数据。
     */
    interface PluginMetadata {
        /** 插件显示名称 */
        String name();
        /** 语义化版本，如 "1.0.0" */
        String version();
        /** 插件描述 */
        String description();
        /** 提供方，默认 "MasterLife" */
        String vendor();
        /** 前端图标 key，如 "BusinessOutline" */
        String icon();
    }

    /**
     * 菜单贡献项。描述一条注入到系统菜单树的菜单记录。
     */
    interface MenuContribution {
        /** 路由路径，如 "/realty/project" */
        String path();
        /** 菜单名称 */
        String name();
        /** 父菜单路径（顶级为空字符串），用于确定在菜单树中的挂载点 */
        String parent();
        /** 前端组件路径，如 "realty/project/index" */
        String component();
        /** 权限标识，如 "realty:project:list" */
        String permission();
        /** 图标 key */
        String icon();
        /** 排序值，越小越靠前 */
        int sort();
    }

    /**
     * 自定义字段模板贡献。描述某类实体（如客户、成交单）的行业预置字段集合。
     */
    interface FieldTemplateContribution {
        /** 绑定的实体类型，如 "customer", "deal" */
        String entityType();
        /** 模板名称，如 "房产客户扩展字段" */
        String templateName();
        /** 字段定义列表 */
        List<FieldDef> fields();

        /** 单个字段定义 */
        interface FieldDef {
            /** 字段 key（英文，程序内唯一） */
            String key();
            /** 字段显示标签（中文） */
            String label();
            /** 字段类型：text/number/select/date/multiselect */
            String fieldType();
            /** 可选配置（JSON 字符串，如 select 的 options，nullable） */
            default String config() { return null; }
        }
    }
}
