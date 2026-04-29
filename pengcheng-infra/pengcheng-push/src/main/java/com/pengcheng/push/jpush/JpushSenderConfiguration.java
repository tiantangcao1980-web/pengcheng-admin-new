package com.pengcheng.push.jpush;

import com.pengcheng.push.unified.ChannelAppSender;
import com.pengcheng.push.unified.PushChannelLogMapper;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPush Sender 装配（E5）
 *
 * <p>仅当 {@code ChannelAppSender} 还未被其他配置注册时，才注册 {@link JpushUnifiedSender}。
 * appKey / masterSecret 从 {@link SystemConfigHelper} 实时读取（配置可热更新）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JpushSenderConfiguration {

    private final SystemConfigHelper configHelper;
    private final PushChannelLogMapper logMapper;

    @Bean
    @ConditionalOnMissingBean(ChannelAppSender.class)
    public ChannelAppSender jpushUnifiedSender() {
        String appKey = configHelper.getPushAppKey();
        String masterSecret = configHelper.getPushMasterSecret();
        log.info("[E5] 注册 JpushUnifiedSender: appKey={}", maskKey(appKey));
        return new JpushUnifiedSender(appKey, masterSecret, logMapper);
    }

    private static String maskKey(String key) {
        if (key == null || key.length() <= 4) return "****";
        return key.substring(0, 4) + "****";
    }
}
