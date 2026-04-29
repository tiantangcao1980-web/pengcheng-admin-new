package com.pengcheng.system.ocr.baidu;

/**
 * OCR 调用异常
 *
 * <p>当百度 OCR HTTP 请求失败、响应不合法或业务错误码非零时抛出。</p>
 */
public class OcrCallException extends RuntimeException {

    private final int errorCode;

    public OcrCallException(String message) {
        super(message);
        this.errorCode = -1;
    }

    public OcrCallException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public OcrCallException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }

    /** 百度 API 返回的 error_code；未知时为 -1 */
    public int getErrorCode() {
        return errorCode;
    }
}
