package com.pengcheng.wechat.subscribe;

/**
 * 基于 Hutool 的微信 HTTP 客户端默认实现
 */
public class HutoolWechatHttpClient implements WechatHttpClient {

    @Override
    public String postJson(String url, String body) {
        try {
            return cn.hutool.http.HttpUtil.createPost(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .execute()
                    .body();
        } catch (Exception e) {
            throw new WechatNetworkException("HTTP 请求失败: " + e.getMessage(), e);
        }
    }
}
