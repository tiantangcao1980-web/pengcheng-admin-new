package com.pengcheng.push.unified;

import java.util.Map;

/**
 * APP 推送渠道发送器（D5 SPI；E5 由 JpushUnifiedSender 实现）。
 *
 * <p>职责：把 push payload 适配为各厂商（极光/友盟/个推）SDK 协议下发到 APP 端。
 * 实现类应注册为 Spring Bean；缺失时 {@link NoOpChannelAppSender} 兜底。
 */
public interface ChannelAppSender {

    /** 渠道编码（jpush / umeng / getui / noop）。 */
    String channelCode();

    /**
     * 是否为兜底 NoOp 实现（无真实推送能力）。
     * 真实渠道返回 false；NoOpChannelAppSender 覆写返回 true。
     */
    default boolean isNoOp() {
        return false;
    }

    /**
     * 发送 APP 通知。
     *
     * @param userId  接收用户 ID（数字字符串）
     * @param title   标题
     * @param content 通知正文
     * @param extras  附加数据（点击跳转参数等，K-V）
     * @return true 调用厂商接口返回 success；false 任何失败（已写 push_channel_log 审计）
     */
    boolean sendToUser(String userId, String title, String content, Map<String, String> extras);
}
