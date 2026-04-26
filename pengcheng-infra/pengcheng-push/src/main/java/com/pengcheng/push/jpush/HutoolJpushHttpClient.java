package com.pengcheng.push.jpush;

/**
 * 基于 Hutool 的极光推送 HTTP 客户端默认实现
 */
public class HutoolJpushHttpClient implements JpushHttpClient {

    @Override
    public String post(String url, String credentials, String body) {
        try {
            return cn.hutool.http.HttpUtil.createPost(url)
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .execute()
                    .body();
        } catch (Exception e) {
            throw new JpushNetworkException("HTTP 请求失败: " + e.getMessage(), e);
        }
    }
}
