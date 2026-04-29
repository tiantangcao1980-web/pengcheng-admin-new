package com.pengcheng.system.ocr;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 名片 OCR Service
 *
 * <p>对接百度 / 腾讯 OCR：调用方传入图片字节，返回结构化的 {@link BusinessCardData}。
 * 当前实现仅做组合，不内联 SDK 调用，以便在没有真实凭据时也可以单元测试。</p>
 */
@Slf4j
public class BusinessCardOcrService {

    private final OcrProvider provider;

    public BusinessCardOcrService(OcrProvider provider) {
        this.provider = provider;
    }

    /**
     * 识别并解析名片
     */
    public BusinessCardData recognize(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("BusinessCardOcrService.recognize: empty image");
            return BusinessCardData.builder().build();
        }
        if (provider == null) {
            log.warn("BusinessCardOcrService: no OcrProvider configured, returning empty result");
            return BusinessCardData.builder().build();
        }
        List<String> lines;
        try {
            lines = provider.recognize(imageBytes);
        } catch (RuntimeException ex) {
            log.warn("OCR provider {} threw: {}", provider.getProviderType(), ex.getMessage());
            return BusinessCardData.builder().build();
        }
        return BusinessCardParser.parse(lines);
    }
}
