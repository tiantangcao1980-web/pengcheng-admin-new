package com.pengcheng.integration.wecom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 企业微信自动配置。
 * <p>
 * Feature Flag：{@code pengcheng.feature.integration.wecom=true}（默认关闭）。
 * 未开启时所有 Wecom Bean 不注入，不影响其他模块。
 */
@Configuration
@ConditionalOnProperty(name = "pengcheng.feature.integration.wecom", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(WecomProperties.class)
public class WecomAutoConfiguration {
    // 由 @EnableConfigurationProperties 自动绑定 WecomProperties；
    // 具体 Wecom Bean（HutoolWecomHttpClient、WecomTokenCache 等）通过 @Component 按需注入。
}
