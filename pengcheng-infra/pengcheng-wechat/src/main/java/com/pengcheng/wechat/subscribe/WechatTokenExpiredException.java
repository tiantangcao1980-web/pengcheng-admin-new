package com.pengcheng.wechat.subscribe;

/**
 * 微信 Access Token 失效异常
 */
public class WechatTokenExpiredException extends RuntimeException {

    public WechatTokenExpiredException(String message) {
        super(message);
    }
}
