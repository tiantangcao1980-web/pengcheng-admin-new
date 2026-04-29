package com.pengcheng.wechat.subscribe;

import com.pengcheng.common.feature.FeatureFlags;
import com.pengcheng.push.unified.ChannelSubscribeSender;
import com.pengcheng.push.unified.NoOpChannelSubscribeSender;
import com.pengcheng.wechat.WechatMiniProgramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信订阅消息 Sender 装配（E5）
 *
 * <p>Feature Flag {@code pengcheng.feature.wechat.mini.enabled=true} 时注册真实 Sender；
 * 否则降级为 NoOp。
 *
 * <p>注意：{@link WechatMiniProgramService} 本身由
 * {@code @ConditionalOnProperty(prefix = FeatureFlags.WECHAT_MINI_PREFIX, name = FeatureFlags.ENABLED, havingValue = "true")}
 * 控制，Flag 关闭时该 Service Bean 不存在，Sender 同步降级。
 */
@Slf4j
@Configuration
public class WechatSubscribeSenderConfiguration {

    @Value("${" + FeatureFlags.WECHAT_MINI_PREFIX + "." + FeatureFlags.ENABLED + ":false}")
    private boolean wechatMiniEnabled;

    @Bean
    @ConditionalOnMissingBean(ChannelSubscribeSender.class)
    public ChannelSubscribeSender wechatMpSubscribeSender(
            org.springframework.beans.factory.ObjectProvider<WechatMiniProgramService> serviceProvider) {

        if (!wechatMiniEnabled) {
            log.info("[E5] wechat.mini Feature Flag=false，WechatMpSubscribeSender 降级为 NoOp");
            return new NoOpChannelSubscribeSender();
        }

        WechatMiniProgramService service = serviceProvider.getIfAvailable();
        if (service == null) {
            log.warn("[E5] WechatMiniProgramService Bean 未找到，WechatMpSubscribeSender 降级为 NoOp");
            return new NoOpChannelSubscribeSender();
        }

        log.info("[E5] 注册 WechatMpSubscribeSender");
        return new WechatMpSubscribeSender(service);
    }
}
