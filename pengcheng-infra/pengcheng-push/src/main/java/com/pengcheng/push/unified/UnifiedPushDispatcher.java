package com.pengcheng.push.unified;

import com.pengcheng.push.PushServiceFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 三通道统一推送调度器（不依赖 Spring 生命周期，可手动 new 用于测试）
 *
 * <p>调用方提供：
 * <ul>
 *     <li>{@link PushServiceFactory}：APP 通道（极光/友盟/个推）</li>
 *     <li>{@link ChannelSubscribeSender}：小程序订阅消息通道</li>
 *     <li>{@link ChannelInboxSender}：Web 站内信通道</li>
 * </ul>
 *
 * <p>策略：先用 {@link PushDecisionRule} 决策出主通道，下发；
 * 主通道失败时（如极光返回 false），按降级链 APP→订阅→站内信 重试一次。</p>
 */
@Slf4j
public class UnifiedPushDispatcher {

    private final PushServiceFactory pushServiceFactory;
    private final ChannelSubscribeSender subscribeSender;
    private final ChannelInboxSender inboxSender;

    /** 是否在主通道失败时降级 */
    private final boolean fallbackEnabled;

    public UnifiedPushDispatcher(PushServiceFactory pushServiceFactory,
                                 ChannelSubscribeSender subscribeSender,
                                 ChannelInboxSender inboxSender) {
        this(pushServiceFactory, subscribeSender, inboxSender, true);
    }

    public UnifiedPushDispatcher(PushServiceFactory pushServiceFactory,
                                 ChannelSubscribeSender subscribeSender,
                                 ChannelInboxSender inboxSender,
                                 boolean fallbackEnabled) {
        this.pushServiceFactory = pushServiceFactory;
        this.subscribeSender = subscribeSender;
        this.inboxSender = inboxSender;
        this.fallbackEnabled = fallbackEnabled;
    }

    /**
     * 调度推送
     *
     * @param target  推送目标
     * @param payload 文本内容（标题/正文/业务字段）
     * @return 调度结果
     */
    public PushDispatchResult dispatch(PushTarget target, PushPayload payload) {
        if (target == null || payload == null) {
            return PushDispatchResult.fail(PushChannel.NONE, "target or payload is null");
        }
        PushChannel primary = PushDecisionRule.decide(target);
        log.info("UnifiedPushDispatcher decided primary channel: userId={}, channel={}",
                target.getUserId(), primary);

        PushDispatchResult result = sendVia(primary, target, payload);
        if (result.isSuccess() || !fallbackEnabled) {
            return result;
        }

        // 失败降级：APP_PUSH → MP_SUBSCRIBE → WEB_INBOX
        for (PushChannel next : fallbackOrder(primary)) {
            log.warn("UnifiedPushDispatcher fallback: from {} to {}, reason={}",
                    primary, next, result.getReason());
            PushDispatchResult fb = sendVia(next, target, payload);
            if (fb.isSuccess()) {
                return fb;
            }
        }
        return result;
    }

    private PushDispatchResult sendVia(PushChannel channel, PushTarget target, PushPayload payload) {
        try {
            switch (channel) {
                case APP_PUSH:
                    return sendApp(target, payload);
                case MP_SUBSCRIBE:
                    return sendSubscribe(target, payload);
                case WEB_INBOX:
                    return sendInbox(target, payload);
                case NONE:
                default:
                    return PushDispatchResult.fail(PushChannel.NONE, "no available channel");
            }
        } catch (RuntimeException ex) {
            log.warn("UnifiedPushDispatcher send via {} threw: {}", channel, ex.getMessage());
            return PushDispatchResult.fail(channel, ex.getClass().getSimpleName() + ":" + ex.getMessage());
        }
    }

    private PushDispatchResult sendApp(PushTarget target, PushPayload payload) {
        if (pushServiceFactory == null) {
            return PushDispatchResult.fail(PushChannel.APP_PUSH, "PushServiceFactory not configured");
        }
        Map<String, String> extras = mergeExtras(target, payload);
        boolean ok = pushServiceFactory.getPushService().pushToUser(
                target.getUserId(), payload.getTitle(), payload.getContent(), extras);
        return ok ? PushDispatchResult.ok(PushChannel.APP_PUSH)
                : PushDispatchResult.fail(PushChannel.APP_PUSH, "provider returned false");
    }

    private PushDispatchResult sendSubscribe(PushTarget target, PushPayload payload) {
        if (subscribeSender == null) {
            return PushDispatchResult.fail(PushChannel.MP_SUBSCRIBE, "subscribe sender not configured");
        }
        if (payload.getSubscribeTemplateId() == null || payload.getSubscribeTemplateId().isBlank()) {
            return PushDispatchResult.fail(PushChannel.MP_SUBSCRIBE, "subscribe templateId missing");
        }
        boolean ok = subscribeSender.send(
                target.getMiniProgramOpenId(),
                payload.getSubscribeTemplateId(),
                payload.getSubscribeData(),
                payload.getSubscribePage());
        return ok ? PushDispatchResult.ok(PushChannel.MP_SUBSCRIBE)
                : PushDispatchResult.fail(PushChannel.MP_SUBSCRIBE, "subscribe send failed");
    }

    private PushDispatchResult sendInbox(PushTarget target, PushPayload payload) {
        if (inboxSender == null) {
            return PushDispatchResult.fail(PushChannel.WEB_INBOX, "inbox sender not configured");
        }
        boolean ok = inboxSender.send(
                target.getUserId(), payload.getTitle(), payload.getContent(),
                payload.getBizType(), payload.getBizId());
        return ok ? PushDispatchResult.ok(PushChannel.WEB_INBOX)
                : PushDispatchResult.fail(PushChannel.WEB_INBOX, "inbox sender returned false");
    }

    private static Map<String, String> mergeExtras(PushTarget target, PushPayload payload) {
        Map<String, String> merged = new HashMap<>();
        if (target.getExtras() != null) {
            merged.putAll(target.getExtras());
        }
        if (payload.getExtras() != null) {
            merged.putAll(payload.getExtras());
        }
        if (payload.getBizType() != null) {
            merged.put("bizType", payload.getBizType());
        }
        if (payload.getBizId() != null) {
            merged.put("bizId", String.valueOf(payload.getBizId()));
        }
        return merged;
    }

    /** 返回从 from 通道开始的降级链（不包含 from 自身） */
    private static PushChannel[] fallbackOrder(PushChannel from) {
        if (from == PushChannel.APP_PUSH) {
            return new PushChannel[]{PushChannel.MP_SUBSCRIBE, PushChannel.WEB_INBOX};
        }
        if (from == PushChannel.MP_SUBSCRIBE) {
            return new PushChannel[]{PushChannel.WEB_INBOX};
        }
        return new PushChannel[]{};
    }
}
