package com.pengcheng.push.unified;

import java.util.Map;

/**
 * 小程序订阅消息发送器
 *
 * <p>由 {@code pengcheng-core/pengcheng-message/subscribe} 子包实现并注入；
 * 在 {@code pengcheng-infra/pengcheng-push} 中只暴露接口，避免下游模块依赖反转。</p>
 *
 * <p>实现类应持有微信小程序 access_token，调用 {@code subscribeMessage.send}。</p>
 */
public interface ChannelSubscribeSender {

    /**
     * 发送订阅消息
     *
     * @param openId      用户 OPENID
     * @param templateId  小程序订阅消息模板 ID
     * @param data        模板参数（key->value）
     * @param page        点击跳转的小程序页面（可空）
     * @return 是否成功（调用微信接口返回 errcode == 0）
     */
    boolean send(String openId, String templateId, Map<String, String> data, String page);
}
