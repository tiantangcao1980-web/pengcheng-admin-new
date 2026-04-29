package com.pengcheng.push.config;

import com.pengcheng.push.unified.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Unified Push SPI 装配（E5 注册真实 Sender，覆盖 D5 NoOp 占位）
 *
 * <p>E2 的 {@code V4MvpAutoConfiguration} 通过 {@code @ConditionalOnMissingBean}
 * 让出位置——本类中的 Bean 同样遵循此约定：若上游已注册（E2），此处不再重复注册。
 *
 * <p>真实 Sender Bean（JPush / WechatSubscribe / WebInbox）在各自模块中声明，
 * 本配置仅负责 NoOp 兜底。
 */
@Slf4j
@Configuration
public class UnifiedPushAutoConfiguration {

    // ==================== NoOp 兜底 ====================
    // 各真实 Sender 在各自 @Configuration 中注册，缺失时由此处 NoOp 兜底。

    @Bean
    @ConditionalOnMissingBean(ChannelAppSender.class)
    public ChannelAppSender noOpChannelAppSender() {
        log.warn("[UnifiedPush] ChannelAppSender 未注册真实实现，使用 NoOp 占位");
        return new NoOpChannelAppSender();
    }

    @Bean
    @ConditionalOnMissingBean(ChannelSubscribeSender.class)
    public ChannelSubscribeSender noOpChannelSubscribeSender() {
        log.warn("[UnifiedPush] ChannelSubscribeSender 未注册真实实现，使用 NoOp 占位");
        return new NoOpChannelSubscribeSender();
    }

    @Bean
    @ConditionalOnMissingBean(ChannelInboxSender.class)
    public ChannelInboxSender noOpChannelInboxSender() {
        log.warn("[UnifiedPush] ChannelInboxSender 未注册真实实现，使用 NoOp 占位");
        return new NoOpChannelInboxSender();
    }
}
