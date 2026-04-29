package com.pengcheng.message.config;

import com.pengcheng.message.inbox.InboxWebSocketBridge;
import com.pengcheng.message.inbox.WebInboxSender;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.push.unified.ChannelInboxSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebInbox Sender 装配（E5）
 *
 * <p>WebSocket bridge 为可选依赖，通过 {@link ObjectProvider} 注入，
 * API 层启动时自动注入；单元测试/无 WebSocket 环境时 bridge=null，
 * Sender 仍可正常落库。
 */
@Slf4j
@Configuration
public class WebInboxSenderConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChannelInboxSender.class)
    public ChannelInboxSender webInboxSender(
            NotificationService notificationService,
            ObjectProvider<InboxWebSocketBridge> bridgeProvider) {

        InboxWebSocketBridge bridge = bridgeProvider.getIfAvailable();
        if (bridge == null) {
            log.info("[E5] InboxWebSocketBridge 未注入，WebInboxSender 将跳过实时推送");
        } else {
            log.info("[E5] 注册 WebInboxSender（含 WebSocket 实时推送）");
        }
        return new WebInboxSender(notificationService, bridge);
    }
}
