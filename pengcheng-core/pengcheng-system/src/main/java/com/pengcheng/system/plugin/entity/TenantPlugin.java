package com.pengcheng.system.plugin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户级插件启用配置，对应 {@code tenant_plugin} 表。
 */
@Data
@TableName("tenant_plugin")
public class TenantPlugin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 插件代码，关联 industry_plugin.code */
    private String pluginCode;

    /** 是否启用（0-禁用 1-启用） */
    private Integer enabled;

    /** 操作人用户ID */
    private Long enabledBy;

    /** 操作时间 */
    private LocalDateTime enabledAt;
}
