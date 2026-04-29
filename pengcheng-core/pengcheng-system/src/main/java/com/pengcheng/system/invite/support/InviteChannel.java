package com.pengcheng.system.invite.support;

/**
 * 邀请渠道常量
 */
public final class InviteChannel {

    private InviteChannel() {
    }

    /** 链接邀请（默认） */
    public static final String LINK = "LINK";

    /** 短信邀请 */
    public static final String SMS = "SMS";

    /** 二维码邀请 */
    public static final String QRCODE = "QRCODE";

    /** Excel 批量导入邀请 */
    public static final String EXCEL = "EXCEL";
}
