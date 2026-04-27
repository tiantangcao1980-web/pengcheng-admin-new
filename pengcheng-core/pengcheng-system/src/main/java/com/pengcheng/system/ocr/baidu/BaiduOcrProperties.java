package com.pengcheng.system.ocr.baidu;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 百度 OCR 配置项
 *
 * <p>在 application.yml 中配置：
 * <pre>
 * pengcheng:
 *   ocr:
 *     baidu:
 *       api-key: YOUR_API_KEY
 *       secret-key: YOUR_SECRET_KEY
 *   feature:
 *     ocr:
 *       baidu: true   # 默认 false，需手动开启
 * </pre>
 */
@ConfigurationProperties(prefix = "pengcheng.ocr.baidu")
public class BaiduOcrProperties {

    /** 百度云应用的 API Key（即 client_id） */
    private String apiKey;

    /** 百度云应用的 Secret Key（即 client_secret） */
    private String secretKey;

    /** OAuth token 端点，默认为百度官方地址 */
    private String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";

    /** 名片 OCR 端点，默认为百度官方地址 */
    private String ocrUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/business_card";

    /** HTTP 超时（毫秒），默认 10 秒 */
    private int timeoutMs = 10_000;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getOcrUrl() {
        return ocrUrl;
    }

    public void setOcrUrl(String ocrUrl) {
        this.ocrUrl = ocrUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
