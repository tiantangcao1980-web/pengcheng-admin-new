package com.pengcheng.wechat.subscribe;

/**
 * 微信 HTTP 客户端 SPI（便于测试 mock）
 */
public interface WechatHttpClient {

    /**
     * POST JSON 请求
     *
     * @param url  完整 URL（含 access_token 参数）
     * @param body JSON 请求体
     * @return 响应体字符串
     * @throws WechatNetworkException 网络层异常
     */
    String postJson(String url, String body);
}
