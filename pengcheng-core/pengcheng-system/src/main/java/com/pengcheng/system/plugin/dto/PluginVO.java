package com.pengcheng.system.plugin.dto;

import lombok.Data;

/**
 * 插件列表视图对象，含租户启用状态。
 */
@Data
public class PluginVO {

    /** 插件代码 */
    private String code;

    /** 显示名称 */
    private String name;

    /** 版本号 */
    private String version;

    /** 描述 */
    private String description;

    /** 提供方 */
    private String vendor;

    /** 前端图标 key */
    private String icon;

    /** 全局启用状态 */
    private Integer globalEnabled;

    /** 当前租户是否已启用（null 表示未查询） */
    private Boolean tenantEnabled;
}
