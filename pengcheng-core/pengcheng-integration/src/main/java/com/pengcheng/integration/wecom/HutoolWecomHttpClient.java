package com.pengcheng.integration.wecom;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 Hutool 的企业微信 HTTP 客户端默认实现。
 * <p>
 * 自动解析 errcode/errmsg，非 0 时抛出 {@link WecomApiException}。
 */
@Slf4j
@Component
public class HutoolWecomHttpClient implements WecomHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS    = 10_000;

    @Override
    public Map<String, Object> get(String url) {
        log.debug("[WecomHttp] GET {}", url);
        try (HttpResponse resp = HttpRequest.get(url)
                .timeout(CONNECT_TIMEOUT_MS)
                .execute()) {
            return parseAndCheck(resp.body());
        }
    }

    @Override
    public Map<String, Object> post(String url, Map<String, Object> body) {
        String json = JSONUtil.toJsonStr(body);
        log.debug("[WecomHttp] POST {} body={}", url, json);
        try (HttpResponse resp = HttpRequest.post(url)
                .timeout(CONNECT_TIMEOUT_MS)
                .body(json)
                .execute()) {
            return parseAndCheck(resp.body());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndCheck(String body) {
        Map<String, Object> map = JSONUtil.toBean(body, Map.class);
        Object errCodeObj = map.get("errcode");
        if (errCodeObj != null) {
            int errCode = ((Number) errCodeObj).intValue();
            if (errCode != 0) {
                String errMsg = String.valueOf(map.getOrDefault("errmsg", "unknown"));
                throw new WecomApiException(errCode, errMsg);
            }
        }
        return map;
    }
}
