package com.pengcheng.integration.wecom;

import java.util.Map;

/**
 * 企业微信 HTTP 客户端 SPI。
 */
public interface WecomHttpClient {

    /**
     * GET 请求，返回解析后的 JSON Map。
     *
     * @param url    完整 URL（含 query 参数）
     * @return 响应 JSON（已验证 errcode==0）
     * @throws WecomApiException 当企业微信返回非 0 errcode 时
     */
    Map<String, Object> get(String url);

    /**
     * POST JSON 请求，返回解析后的 JSON Map。
     *
     * @param url    完整 URL
     * @param body   请求体 Map（自动序列化为 JSON）
     * @return 响应 JSON（已验证 errcode==0）
     * @throws WecomApiException 当企业微信返回非 0 errcode 时
     */
    Map<String, Object> post(String url, Map<String, Object> body);
}
