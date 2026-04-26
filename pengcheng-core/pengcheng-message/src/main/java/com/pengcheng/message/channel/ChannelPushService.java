package com.pengcheng.message.channel;

import com.pengcheng.message.subscribe.SubscribeMessageRequest;
import com.pengcheng.message.subscribe.SubscribeMessageService;
import com.pengcheng.push.PushServiceFactory;
import com.pengcheng.push.unified.ChannelInboxSender;
import com.pengcheng.push.unified.ChannelSubscribeSender;
import com.pengcheng.push.unified.PushChannel;
import com.pengcheng.push.unified.PushDispatchResult;
import com.pengcheng.push.unified.PushPayload;
import com.pengcheng.push.unified.PushTarget;
import com.pengcheng.push.unified.UnifiedPushDispatcher;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 三通道统一调度业务 Service（V4.0 闭环⑤ 9）
 *
 * <p>使用方式：
 * <pre>
 *     channelPushService.push(userId, ChannelPushRequest.builder()
 *             .bizType("approval").bizId(99L)
 *             .title("审批通知").content("您有新的审批待处理")
 *             .build());
 * </pre>
 *
 * <p>不在此处直接持有 Spring 注解（使用普通构造注入），由 starter 模块
 * 在 Configuration 中装配。这样可以无 Spring 上下文做单测。</p>
 */
@Slf4j
public class ChannelPushService {

    private final UserChannelResolver channelResolver;
    private final UnifiedPushDispatcher dispatcher;
    private final PushChannelLogStore logStore;

    public ChannelPushService(UserChannelResolver channelResolver,
                              UnifiedPushDispatcher dispatcher,
                              PushChannelLogStore logStore) {
        this.channelResolver = channelResolver;
        this.dispatcher = dispatcher;
        this.logStore = logStore;
    }

    /** 便捷构造：使用各组件直接装配 */
    public static ChannelPushService create(UserChannelResolver resolver,
                                            PushServiceFactory pushFactory,
                                            ChannelSubscribeSender subscribeSender,
                                            ChannelInboxSender inboxSender,
                                            PushChannelLogStore logStore) {
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(pushFactory, subscribeSender, inboxSender);
        return new ChannelPushService(resolver, dispatcher, logStore);
    }

    /**
     * 单用户推送
     */
    public PushDispatchResult push(Long userId, ChannelPushRequest request) {
        if (userId == null || request == null) {
            return PushDispatchResult.fail(PushChannel.NONE, "userId or request is null");
        }
        UserChannelProfile profile = channelResolver.resolve(userId);
        if (profile == null) {
            profile = UserChannelProfile.builder().userId(userId).webInboxEnabled(true).build();
        }
        PushTarget target = PushTarget.builder()
                .userId(String.valueOf(userId))
                .registrationId(profile.getAppRegistrationId())
                .appOnline(profile.isAppOnline())
                .miniProgramOpenId(profile.getMiniProgramOpenId())
                .miniProgramSubscribed(profile.isMiniProgramSubscribed())
                .webInboxEnabled(profile.isWebInboxEnabled())
                .build();
        PushPayload payload = PushPayload.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .subscribeTemplateId(request.getSubscribeTemplateId())
                .subscribeData(request.getSubscribeData())
                .subscribePage(request.getSubscribePage())
                .extras(request.getExtras())
                .build();

        PushDispatchResult result = dispatcher.dispatch(target, payload);
        record(userId, request, result);
        return result;
    }

    private void record(Long userId, ChannelPushRequest request, PushDispatchResult result) {
        try {
            PushChannelLog log = PushChannelLog.builder()
                    .userId(userId)
                    .channel(result.getChannel() == null ? PushChannel.NONE.getCode() : result.getChannel().getCode())
                    .bizType(request.getBizType())
                    .bizId(request.getBizId())
                    .title(request.getTitle())
                    .success(result.isSuccess() ? 1 : 0)
                    .reason(result.getReason())
                    .subscribeTemplateId(request.getSubscribeTemplateId())
                    .createTime(LocalDateTime.now())
                    .build();
            logStore.save(log);
        } catch (RuntimeException ex) {
            // 日志失败不影响业务
            ChannelPushService.log.warn("push channel log save failed: {}", ex.getMessage());
        }
    }

    /** 直接通过订阅消息子模块发送（用户已知 OPENID + 模板渲染） */
    public boolean sendSubscribe(SubscribeMessageService service, SubscribeMessageRequest req) {
        if (service == null || req == null) {
            return false;
        }
        return service.send(req);
    }
}
