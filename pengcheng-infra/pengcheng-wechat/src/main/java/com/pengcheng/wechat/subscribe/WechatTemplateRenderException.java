package com.pengcheng.wechat.subscribe;

/**
 * 微信订阅消息模板渲染异常
 */
public class WechatTemplateRenderException extends RuntimeException {

    public WechatTemplateRenderException(String message) {
        super(message);
    }
}
