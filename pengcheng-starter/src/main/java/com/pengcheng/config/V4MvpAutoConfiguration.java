package com.pengcheng.config;

import com.pengcheng.message.channel.ChannelPushService;
import com.pengcheng.message.channel.PushChannelLog;
import com.pengcheng.message.channel.PushChannelLogStore;
import com.pengcheng.message.channel.UserChannelProfile;
import com.pengcheng.message.channel.UserChannelResolver;
import com.pengcheng.push.PushServiceFactory;
import com.pengcheng.push.unified.ChannelInboxSender;
import com.pengcheng.push.unified.ChannelSubscribeSender;
import com.pengcheng.push.unified.UnifiedPushDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * V4 MVP 装配总线（F3）。
 *
 * <p>负责把 D5 的"无 Spring 生命周期" POJO 类（{@link UnifiedPushDispatcher}、
 * {@link ChannelPushService}）以及尚无具体实现的接口（{@link UserChannelResolver}、
 * {@link PushChannelLogStore}）装配为 Spring Bean。
 *
 * <p>NoOp 兜底原则：
 * <ul>
 *   <li>{@link UserChannelResolver} 在没有 sys_user_device 心跳数据集成前，
 *       默认返回仅启用站内信通道的画像；</li>
 *   <li>{@link PushChannelLogStore} 在 F4 push_channel_log 落库 PR 之前，
 *       仅打印日志（避免落库失败导致推送主路径异常）；</li>
 *   <li>真实实现可由后续 PR 通过 {@code @ConditionalOnMissingBean} 自动覆盖。</li>
 * </ul>
 *
 * <p>SPI（{@link ChannelInboxSender} / {@link ChannelSubscribeSender}）的 NoOp 兜底
 * 已由 {@code com.pengcheng.push.config.UnifiedPushAutoConfiguration} 提供。
 */
@Slf4j
@Configuration
public class V4MvpAutoConfiguration {

    /** 三通道统一推送调度器 — 注入 SPI 三件套（缺失时由 UnifiedPushAutoConfiguration NoOp 兜底）。 */
    @Bean
    @ConditionalOnMissingBean
    public UnifiedPushDispatcher unifiedPushDispatcher(
            PushServiceFactory pushServiceFactory,
            ChannelSubscribeSender subscribeSender,
            ChannelInboxSender inboxSender) {
        log.info("[V4MvpAutoConfiguration] 注册 UnifiedPushDispatcher Bean");
        return new UnifiedPushDispatcher(pushServiceFactory, subscribeSender, inboxSender);
    }

    /** 三通道决策业务 Service。 */
    @Bean
    @ConditionalOnMissingBean
    public ChannelPushService channelPushService(
            UserChannelResolver channelResolver,
            UnifiedPushDispatcher dispatcher,
            PushChannelLogStore logStore) {
        log.info("[V4MvpAutoConfiguration] 注册 ChannelPushService Bean");
        return new ChannelPushService(channelResolver, dispatcher, logStore);
    }

    /**
     * 默认 UserChannelResolver — 在 sys_user_device 集成前的兜底实现。
     * 返回仅启用站内信的画像，确保推送链路有可用通道。
     */
    @Bean
    @ConditionalOnMissingBean
    public UserChannelResolver defaultUserChannelResolver() {
        log.warn("[V4MvpAutoConfiguration] 未注册真实 UserChannelResolver，使用站内信兜底实现 — "
                + "请在 pengcheng-system 或 pengcheng-message 提供基于 sys_user_device 的 Resolver。");
        return userId -> UserChannelProfile.builder()
                .userId(userId)
                .appOnline(false)
                .miniProgramSubscribed(false)
                .webInboxEnabled(true)
                .build();
    }

    /**
     * 默认 PushChannelLogStore — 在 F4 落库 PR 之前的兜底实现，仅打印日志。
     * push_channel_log 表（V55）已建好，落库实现见 follow-up 工单 F4。
     */
    @Bean
    @ConditionalOnMissingBean
    public PushChannelLogStore defaultPushChannelLogStore() {
        log.warn("[V4MvpAutoConfiguration] 未注册真实 PushChannelLogStore，使用日志兜底实现 — "
                + "F4 工单将提供 push_channel_log Mapper-based 实现。");
        return new LoggingPushChannelLogStore();
    }

    /** 日志兜底实现（不落库，只打 INFO/WARN）。 */
    @Slf4j
    static class LoggingPushChannelLogStore implements PushChannelLogStore {
        @Override
        public void save(PushChannelLog logEntry) {
            if (logEntry == null) {
                return;
            }
            boolean ok = logEntry.getSuccess() != null && logEntry.getSuccess() == 1;
            if (ok) {
                log.info("[push_channel_log] OK userId={} channel={} bizType={} bizId={}",
                        logEntry.getUserId(), logEntry.getChannel(),
                        logEntry.getBizType(), logEntry.getBizId());
            } else {
                log.warn("[push_channel_log] FAIL userId={} channel={} bizType={} reason={}",
                        logEntry.getUserId(), logEntry.getChannel(),
                        logEntry.getBizType(), logEntry.getReason());
            }
        }
    }
}
