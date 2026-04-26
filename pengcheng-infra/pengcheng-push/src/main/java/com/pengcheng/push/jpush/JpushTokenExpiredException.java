package com.pengcheng.push.jpush;

/**
 * 极光推送 Token/凭证失效异常
 */
public class JpushTokenExpiredException extends RuntimeException {

    public JpushTokenExpiredException(String message) {
        super(message);
    }
}
