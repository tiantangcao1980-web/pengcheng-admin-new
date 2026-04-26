package com.pengcheng.push.unified;

/**
 * 三通道决策规则（纯函数）
 *
 * <p>决策顺序（PRD 闭环 ⑤ 7）：
 * <ol>
 *     <li>APP 在线（registrationId 非空 + appOnline）→ APP_PUSH</li>
 *     <li>APP 离线但有注册 ID → APP_PUSH（走静默推送，由极光/友盟尝试唤醒）</li>
 *     <li>小程序订阅有效 → MP_SUBSCRIBE</li>
 *     <li>Web 站内信启用 → WEB_INBOX</li>
 *     <li>否则 → NONE（调用方记录失败）</li>
 * </ol>
 *
 * <p>该类拆分出来便于单测：覆盖 5 条主分支即可达成 60%+ 行覆盖率。</p>
 */
public final class PushDecisionRule {

    private PushDecisionRule() {
    }

    public static PushChannel decide(PushTarget target) {
        if (target == null) {
            return PushChannel.NONE;
        }
        boolean hasApp = isNotBlank(target.getRegistrationId());
        if (hasApp && target.isAppOnline()) {
            return PushChannel.APP_PUSH;
        }
        if (hasApp) {
            // APP 已注册但当前离线：仍优先尝试推送（厂商通道会做唤醒/暂存）
            return PushChannel.APP_PUSH;
        }
        if (isNotBlank(target.getMiniProgramOpenId()) && target.isMiniProgramSubscribed()) {
            return PushChannel.MP_SUBSCRIBE;
        }
        if (target.isWebInboxEnabled()) {
            return PushChannel.WEB_INBOX;
        }
        return PushChannel.NONE;
    }

    /**
     * 在 APP 离线场景下，如果业务期望"离线必达"，则跳过 APP_PUSH，直接走订阅消息或站内信。
     *
     * <p>主要用于业务关键消息（审批/告警），由调用方传 forceFallback=true 调用本方法。</p>
     */
    public static PushChannel decideStrict(PushTarget target) {
        if (target == null) {
            return PushChannel.NONE;
        }
        boolean hasApp = isNotBlank(target.getRegistrationId());
        if (hasApp && target.isAppOnline()) {
            return PushChannel.APP_PUSH;
        }
        if (isNotBlank(target.getMiniProgramOpenId()) && target.isMiniProgramSubscribed()) {
            return PushChannel.MP_SUBSCRIBE;
        }
        if (target.isWebInboxEnabled()) {
            return PushChannel.WEB_INBOX;
        }
        if (hasApp) {
            // 兜底：没小程序也没站内信 → 仍尝试 APP（即便离线）
            return PushChannel.APP_PUSH;
        }
        return PushChannel.NONE;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
