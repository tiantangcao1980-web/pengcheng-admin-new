package com.pengcheng.wechat.subscribe;

/**
 * 微信 HTTP 网络异常
 */
public class WechatNetworkException extends RuntimeException {

    public WechatNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
