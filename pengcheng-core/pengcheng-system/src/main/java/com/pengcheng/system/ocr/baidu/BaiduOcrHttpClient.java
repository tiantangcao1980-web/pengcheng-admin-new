package com.pengcheng.system.ocr.baidu;

/**
 * 百度 OCR HTTP 客户端接口
 *
 * <p>独立接口便于单元测试 Mock，生产环境由
 * {@link HutoolBaiduOcrHttpClient} 实现。</p>
 */
public interface BaiduOcrHttpClient {

    /**
     * 获取 OAuth2 access_token
     *
     * @param tokenUrl  token 端点 URL
     * @param apiKey    client_id
     * @param secretKey client_secret
     * @return token 响应 JSON 字符串（包含 access_token / expires_in 等字段）
     * @throws OcrCallException HTTP 请求失败时抛出
     */
    String fetchToken(String tokenUrl, String apiKey, String secretKey);

    /**
     * 调用名片 OCR 接口
     *
     * @param ocrUrl      名片 OCR 端点 URL
     * @param accessToken 有效的 access_token
     * @param imageBase64 图片 Base64 编码字符串（不含 data URI 前缀）
     * @return OCR 响应 JSON 字符串
     * @throws OcrCallException HTTP 请求失败时抛出
     */
    String recognizeBusinessCard(String ocrUrl, String accessToken, String imageBase64);
}
