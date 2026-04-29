package com.pengcheng.integration.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IM 集成租户配置实体（对应 integration_provider_config）。
 */
@Data
@TableName("integration_provider_config")
public class IntegrationProviderConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** provider 标识：wecom / dingtalk / feishu */
    private String provider;

    /** 企业 ID（企业微信 corpId / 钉钉 corpId / 飞书 appId） */
    private String corpId;

    /** 应用 ID */
    private String agentId;

    /** sys_secret_vault 引用键（不直接存 secret） */
    private String secretRef;

    /** 是否开启同步 */
    private Integer syncEnabled;

    private LocalDateTime lastSyncTime;

    private String lastSyncStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
