package com.pengcheng.finance.contract.sign.esign;

/**
 * e签宝 API 调用异常。
 * <p>
 * 包含 HTTP 状态码和 e签宝业务错误码（code），便于上层服务判断重试策略。
 */
public class EsignCallException extends RuntimeException {

    private final int httpStatus;
    private final String esignCode;

    public EsignCallException(String message) {
        super(message);
        this.httpStatus = -1;
        this.esignCode = null;
    }

    public EsignCallException(String message, int httpStatus, String esignCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.esignCode = esignCode;
    }

    public EsignCallException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
        this.esignCode = null;
    }

    /** HTTP 响应状态码，-1 表示未获取到（如连接超时等） */
    public int getHttpStatus() {
        return httpStatus;
    }

    /** e签宝业务错误码，如 "000000" 表示成功，null 表示 HTTP 层错误 */
    public String getEsignCode() {
        return esignCode;
    }
}
