package com.pengcheng.push.jpush;

/**
 * 极光推送网络异常
 */
public class JpushNetworkException extends RuntimeException {

    public JpushNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
