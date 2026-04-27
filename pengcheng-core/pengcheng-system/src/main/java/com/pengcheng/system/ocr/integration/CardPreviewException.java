package com.pengcheng.system.ocr.integration;

/**
 * 名片预览异常
 *
 * <p>当 OCR 调用失败或图片无法处理时抛出，由 Controller 层统一转换为 HTTP 错误响应。</p>
 */
public class CardPreviewException extends RuntimeException {

    public CardPreviewException(String message) {
        super(message);
    }

    public CardPreviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
