package com.pengcheng.finance.contract.sign.esign;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSignFlowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 Hutool 的 e签宝 HTTP 客户端默认实现。
 *
 * <h3>e签宝 V3 签名算法摘要</h3>
 * <ol>
 *   <li>构造待签字符串：<br>
 *       {@code HTTPMethod + "\n" + Content-MD5 + "\n" + Content-Type + "\n" + Date + "\n" + CanonicalizedHeaders + CanonicalizedResource}
 *   </li>
 *   <li>用 {@code appSecret} 做 {@code HMAC-SHA256}，再用 {@code Base64} 编码得到签名值。</li>
 *   <li>请求头携带：
 *     <ul>
 *       <li>{@code X-Tsign-Open-App-Id}: appId</li>
 *       <li>{@code X-Tsign-Open-Auth-Mode}: Signature</li>
 *       <li>{@code X-Tsign-Open-Ca-Timestamp}: 当前 Unix 毫秒</li>
 *       <li>{@code X-Tsign-Open-Ca-Nonce}: 随机 UUID</li>
 *       <li>{@code X-Tsign-Open-Ca-Signature}: Base64(HMAC-SHA256(stringToSign, appSecret))</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>注意：e签宝正式 API 要求 Content-MD5 = Base64(MD5(requestBody))，
 * GET 请求 body 为空时 Content-MD5 为空字符串。
 */
@Slf4j
@RequiredArgsConstructor
public class HutoolEsignHttpClient implements EsignHttpClient {

    /** e签宝业务成功码 */
    private static final String ESIGN_SUCCESS_CODE = "000000";

    private final EsignProperties properties;

    // =========================================================
    // 接口实现
    // =========================================================

    @Override
    public String createSignFlow(EsignSignFlowRequest request) {
        String url = properties.getApiHost() + "/v3/sign-flow/create-by-doc";
        String body = JSONUtil.toJsonStr(request);
        String responseBody = doPost(url, body);
        JSONObject data = parseData(responseBody, url);
        return data.getStr("signFlowId");
    }

    @Override
    public void addDoc(String signFlowId, String fileId, String fileName) {
        String url = properties.getApiHost() + "/v3/sign-flow/" + signFlowId + "/docs";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fileId", fileId);
        payload.put("fileName", fileName);
        String body = JSONUtil.toJsonStr(payload);
        doPost(url, body);
    }

    @Override
    public String getSignUrl(String signFlowId, String signerId) {
        String url = properties.getApiHost() + "/v3/sign-flow/" + signFlowId + "/sign-url?signerId=" + signerId;
        String responseBody = doGet(url);
        JSONObject data = parseData(responseBody, url);
        return data.getStr("signUrl");
    }

    @Override
    public String queryFlowStatus(String signFlowId) {
        String url = properties.getApiHost() + "/v3/sign-flow/" + signFlowId + "/detail";
        String responseBody = doGet(url);
        JSONObject data = parseData(responseBody, url);
        return data.getStr("signFlowStatus");
    }

    // =========================================================
    // 私有 HTTP 工具方法
    // =========================================================

    /**
     * 发送 POST 请求。
     *
     * @param url  完整 URL
     * @param body JSON 请求体
     * @return 响应原始 JSON 字符串
     */
    private String doPost(String url, String body) {
        String contentType = "application/json; charset=UTF-8";
        String contentMd5 = calcContentMd5(body);
        Map<String, String> headers = buildAuthHeaders("POST", contentMd5, contentType, url);

        try (HttpResponse response = HttpRequest.post(url)
                .headerMap(headers, true)
                .header("Content-Type", contentType)
                .header("Content-MD5", contentMd5)
                .body(body)
                .timeout(10_000)
                .execute()) {
            return checkHttpStatus(response, url);
        } catch (EsignCallException e) {
            throw e;
        } catch (Exception e) {
            throw new EsignCallException("e签宝 POST 请求异常 url=" + url, e);
        }
    }

    /**
     * 发送 GET 请求。
     *
     * @param url 完整 URL（含查询参数）
     * @return 响应原始 JSON 字符串
     */
    private String doGet(String url) {
        Map<String, String> headers = buildAuthHeaders("GET", "", "application/json", url);

        try (HttpResponse response = HttpRequest.get(url)
                .headerMap(headers, true)
                .header("Content-Type", "application/json")
                .timeout(10_000)
                .execute()) {
            return checkHttpStatus(response, url);
        } catch (EsignCallException e) {
            throw e;
        } catch (Exception e) {
            throw new EsignCallException("e签宝 GET 请求异常 url=" + url, e);
        }
    }

    /**
     * 校验 HTTP 状态码，非 2xx 抛出 {@link EsignCallException}。
     *
     * @param response HTTP 响应
     * @param url      请求 URL（用于日志）
     * @return 响应体字符串
     */
    private String checkHttpStatus(HttpResponse response, String url) {
        int status = response.getStatus();
        String body = response.body();
        if (status < 200 || status >= 300) {
            log.error("[Esign] HTTP 错误 status={} url={} body={}", status, url, body);
            throw new EsignCallException("e签宝接口 HTTP 错误 status=" + status, status, null);
        }
        return body;
    }

    /**
     * 解析 e签宝响应，检查业务 code，返回 data 节点。
     *
     * @param responseBody 响应原始 JSON
     * @param url          请求 URL（用于日志）
     * @return data JSON 对象
     */
    private JSONObject parseData(String responseBody, String url) {
        JSONObject json;
        try {
            json = JSONUtil.parseObj(responseBody);
        } catch (Exception e) {
            throw new EsignCallException("e签宝响应非 JSON url=" + url + " body=" + responseBody);
        }
        String code = json.getStr("code");
        if (!ESIGN_SUCCESS_CODE.equals(code)) {
            String message = json.getStr("message", "unknown");
            log.error("[Esign] 业务错误 code={} message={} url={}", code, message, url);
            throw new EsignCallException("e签宝业务错误 code=" + code + " message=" + message, 200, code);
        }
        JSONObject data = json.getJSONObject("data");
        return data != null ? data : new JSONObject();
    }

    /**
     * 构造 e签宝 V3 鉴权请求头。
     * <p>
     * 待签字符串格式（e签宝 V3 规范）：
     * <pre>
     * HTTPMethod\n
     * Content-MD5\n
     * Content-Type\n
     * X-Tsign-Open-Ca-Timestamp\n
     * X-Tsign-Open-App-Id:{appId}\n
     * RequestURI
     * </pre>
     *
     * @param method      HTTP 方法（GET/POST）
     * @param contentMd5  请求体 MD5（GET 时传空字符串）
     * @param contentType Content-Type 头值
     * @param fullUrl     完整 URL（用于提取 path+query）
     * @return 鉴权相关请求头 Map
     */
    Map<String, String> buildAuthHeaders(String method, String contentMd5, String contentType, String fullUrl) {
        long timestamp = System.currentTimeMillis();
        String nonce = cn.hutool.core.lang.UUID.fastUUID().toString(true);

        // 提取 path+query（去掉 scheme+host）
        String requestUri = extractPathAndQuery(fullUrl);

        String stringToSign = method + "\n"
                + contentMd5 + "\n"
                + contentType + "\n"
                + timestamp + "\n"
                + "X-Tsign-Open-App-Id:" + properties.getAppId() + "\n"
                + requestUri;

        String signature = calcHmacSha256(stringToSign, properties.getAppSecret());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Tsign-Open-App-Id", properties.getAppId());
        headers.put("X-Tsign-Open-Auth-Mode", "Signature");
        headers.put("X-Tsign-Open-Ca-Timestamp", String.valueOf(timestamp));
        headers.put("X-Tsign-Open-Ca-Nonce", nonce);
        headers.put("X-Tsign-Open-Ca-Signature", signature);
        return headers;
    }

    /**
     * 计算请求体的 Base64(MD5(body)) — e签宝要求的 Content-MD5。
     *
     * @param body 请求体字符串（UTF-8）
     * @return Base64 编码的 MD5 摘要；body 为空时返回空字符串
     */
    private String calcContentMd5(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        byte[] md5 = cn.hutool.crypto.digest.DigestUtil.md5(body.getBytes(StandardCharsets.UTF_8));
        return Base64.encode(md5);
    }

    /**
     * HMAC-SHA256 签名，返回 Base64 编码结果。
     *
     * @param data      待签字符串
     * @param secretKey 签名密钥（appSecret）
     * @return Base64 编码后的签名
     */
    String calcHmacSha256(String data, String secretKey) {
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] signBytes = hmac.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.encode(signBytes);
    }

    /**
     * 从完整 URL 中提取 path + query 部分（去掉 scheme://host）。
     *
     * @param fullUrl 完整 URL
     * @return /path?query 形式的字符串
     */
    private String extractPathAndQuery(String fullUrl) {
        // 去掉 scheme://host 前缀
        int schemeEnd = fullUrl.indexOf("://");
        if (schemeEnd < 0) {
            return fullUrl;
        }
        int pathStart = fullUrl.indexOf('/', schemeEnd + 3);
        if (pathStart < 0) {
            return "/";
        }
        return fullUrl.substring(pathStart);
    }
}
