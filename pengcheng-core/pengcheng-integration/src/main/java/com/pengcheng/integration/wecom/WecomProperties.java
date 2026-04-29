package com.pengcheng.integration.wecom;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 企业微信全局配置属性（从 Spring Environment 读取，仅作默认值；
 * 多租户实际配置以 integration_provider_config 表为准）。
 */
@Data
@ConfigurationProperties(prefix = "pengcheng.integration.wecom")
public class WecomProperties {

    /** 企业 ID（全局默认，多租户场景由 DB 覆盖） */
    private String corpId;

    /** 应用 AgentId */
    private String agentId;

    /** 应用 Secret（生产建议走 KMS，此处仅便于本地调试） */
    private String secret;

    /** OAuth 回调地址 */
    private String callbackUrl;
}
