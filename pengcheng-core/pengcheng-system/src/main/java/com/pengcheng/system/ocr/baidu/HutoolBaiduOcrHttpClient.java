package com.pengcheng.system.ocr.baidu;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 Hutool 的百度 OCR HTTP 客户端默认实现
 */
@Slf4j
public class HutoolBaiduOcrHttpClient implements BaiduOcrHttpClient {

    private final int timeoutMs;

    public HutoolBaiduOcrHttpClient(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String fetchToken(String tokenUrl, String apiKey, String secretKey) {
        String url = tokenUrl
                + "?grant_type=client_credentials"
                + "&client_id=" + apiKey
                + "&client_secret=" + secretKey;
        log.debug("百度 OCR fetchToken: {}", tokenUrl);
        try (HttpResponse resp = HttpRequest.post(url)
                .timeout(timeoutMs)
                .execute()) {
            if (!resp.isOk()) {
                throw new OcrCallException(
                        "百度 OCR token 请求失败，HTTP status=" + resp.getStatus());
            }
            return resp.body();
        } catch (OcrCallException e) {
            throw e;
        } catch (Exception e) {
            throw new OcrCallException("百度 OCR token 请求异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String recognizeBusinessCard(String ocrUrl, String accessToken, String imageBase64) {
        String url = ocrUrl + "?access_token=" + accessToken;
        log.debug("百度 OCR recognizeBusinessCard: {}", ocrUrl);
        try (HttpResponse resp = HttpRequest.post(url)
                .contentType("application/x-www-form-urlencoded")
                .timeout(timeoutMs)
                .form("image", imageBase64)
                .execute()) {
            if (!resp.isOk()) {
                throw new OcrCallException(
                        "百度名片 OCR 请求失败，HTTP status=" + resp.getStatus());
            }
            return resp.body();
        } catch (OcrCallException e) {
            throw e;
        } catch (Exception e) {
            throw new OcrCallException("百度名片 OCR 请求异常: " + e.getMessage(), e);
        }
    }
}
