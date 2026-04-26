package com.pengcheng.push.unified;

/**
 * 统一推送通道枚举
 *
 * <p>三通道含义：
 * <ul>
 *     <li>{@link #APP_PUSH}：极光/友盟/个推等原生 APP 推送，要求设备在线或允许唤醒</li>
 *     <li>{@link #MP_SUBSCRIBE}：微信小程序订阅消息，要求用户曾经授权且有有效 OPENID</li>
 *     <li>{@link #WEB_INBOX}：Web 站内信兜底，记录到 sys_notification 由前端拉取</li>
 * </ul>
 */
public enum PushChannel {

    APP_PUSH("appPush", "APP 推送"),
    MP_SUBSCRIBE("mpSubscribe", "小程序订阅消息"),
    WEB_INBOX("webInbox", "Web 站内信"),
    /** 决策结果为"无可用通道"时使用，调用方应记录失败 */
    NONE("none", "无可用通道");

    private final String code;
    private final String displayName;

    PushChannel(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
