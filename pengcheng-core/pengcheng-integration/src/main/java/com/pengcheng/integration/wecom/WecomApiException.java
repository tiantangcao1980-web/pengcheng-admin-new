package com.pengcheng.integration.wecom;

/**
 * 企业微信 API 调用异常（errcode != 0）。
 */
public class WecomApiException extends RuntimeException {

    private final int errCode;

    public WecomApiException(int errCode, String errMsg) {
        super("[Wecom] errcode=" + errCode + ", errmsg=" + errMsg);
        this.errCode = errCode;
    }

    public int getErrCode() {
        return errCode;
    }
}
