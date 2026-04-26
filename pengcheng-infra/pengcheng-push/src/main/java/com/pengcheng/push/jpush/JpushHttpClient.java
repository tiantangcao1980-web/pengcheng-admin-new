package com.pengcheng.push.jpush;

/**
 * 极光推送 HTTP 客户端 SPI（便于测试 mock）
 */
public interface JpushHttpClient {

    /**
     * POST 请求
     *
     * @param url         目标 URL
     * @param credentials Basic Auth 凭证（已 Base64 编码）
     * @param body        JSON 请求体
     * @return 响应体字符串
     * @throws JpushNetworkException 网络层异常
     */
    String post(String url, String credentials, String body);
}
