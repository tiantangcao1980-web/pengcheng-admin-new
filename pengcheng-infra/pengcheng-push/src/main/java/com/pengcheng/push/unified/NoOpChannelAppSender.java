package com.pengcheng.push.unified;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * APP 推送渠道兜底实现（无第三方推送 SDK 时使用）。
 *
 * <p>启用真实 sender（如 JPush）时，由 {@code @ConditionalOnMissingBean} 让出位置。
 */
@Slf4j
public class NoOpChannelAppSender implements ChannelAppSender {

    @Override
    public String channelCode() {
        return "noop";
    }

    @Override
    public boolean sendToUser(String userId, String title, String content, Map<String, String> extras) {
        log.warn("[NoOpAppSender] APP 推送被丢弃（无真实 sender）：userId={}, title={}", userId, title);
        return false;
    }
}
