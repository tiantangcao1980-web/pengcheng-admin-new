package com.pengcheng.push.unified;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 订阅消息渠道兜底实现（无微信小程序配置时使用）。
 */
@Slf4j
public class NoOpChannelSubscribeSender implements ChannelSubscribeSender {

    @Override
    public boolean send(String openId, String templateId, Map<String, String> data, String page) {
        log.warn("[NoOpSubscribeSender] 订阅消息被丢弃（无真实 sender）：openId={}, template={}", openId, templateId);
        return false;
    }
}
