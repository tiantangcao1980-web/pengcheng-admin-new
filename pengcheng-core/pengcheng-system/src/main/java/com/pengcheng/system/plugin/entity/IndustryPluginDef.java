package com.pengcheng.system.plugin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行业插件注册表实体，对应 {@code industry_plugin} 表。
 */
@Data
@TableName("industry_plugin")
public class IndustryPluginDef {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 插件唯一编码 */
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

    /**
     * 全局启用状态（0-禁用 1-启用）。
     * syncToDb 时不覆盖此字段，保留管理员手动设置值。
     */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
