package com.pengcheng.config;

import com.pengcheng.message.channel.ChannelPushService;
import com.pengcheng.message.channel.PushChannelLog;
import com.pengcheng.message.channel.PushChannelLogStore;
import com.pengcheng.message.channel.UserChannelProfile;
import com.pengcheng.message.channel.UserChannelResolver;
import com.pengcheng.push.PushServiceFactory;
import com.pengcheng.push.unified.ChannelInboxSender;
import com.pengcheng.push.unified.ChannelSubscribeSender;
import com.pengcheng.push.unified.NoOpChannelInboxSender;
import com.pengcheng.push.unified.NoOpChannelSubscribeSender;
import com.pengcheng.push.unified.UnifiedPushDispatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * F3 装配冒烟测：直接调用 {@link V4MvpAutoConfiguration} 的 @Bean 工厂方法，
 * 验证 Bean 创建无 NPE、构造参数装配正确，且兜底实现行为符合预期。
 *
 * <p>不启动 Spring 上下文（避免依赖 DB/Redis/Quartz），保证测试在沙箱环境也能跑。
 */
class V4MvpAutoConfigurationTest {

    private final V4MvpAutoConfiguration cfg = new V4MvpAutoConfiguration();

    @Test
    void unifiedPushDispatcher_should_be_built_with_three_senders() {
        ChannelSubscribeSender subscribeSender = new NoOpChannelSubscribeSender();
        ChannelInboxSender inboxSender = new NoOpChannelInboxSender();
        PushServiceFactory factory = mock(PushServiceFactory.class);

        UnifiedPushDispatcher dispatcher = cfg.unifiedPushDispatcher(factory, subscribeSender, inboxSender);

        assertNotNull(dispatcher);
    }

    @Test
    void channelPushService_should_be_built_with_resolver_dispatcher_logstore() {
        UserChannelResolver resolver = userId -> UserChannelProfile.builder().userId(userId).build();
        UnifiedPushDispatcher dispatcher = mock(UnifiedPushDispatcher.class);
        PushChannelLogStore logStore = log -> {};

        ChannelPushService service = cfg.channelPushService(resolver, dispatcher, logStore);

        assertNotNull(service);
    }

    @Test
    void defaultUserChannelResolver_should_return_inbox_only_profile() {
        UserChannelResolver resolver = cfg.defaultUserChannelResolver();
        UserChannelProfile profile = resolver.resolve(42L);

        assertNotNull(profile);
        assertEquals(42L, profile.getUserId());
        assertFalse(profile.isAppOnline());
        assertFalse(profile.isMiniProgramSubscribed());
        assertTrue(profile.isWebInboxEnabled());
    }

    @Test
    void defaultPushChannelLogStore_should_swallow_null_and_log_only() {
        PushChannelLogStore store = cfg.defaultPushChannelLogStore();
        // 兜底实现仅打印日志，不抛异常
        store.save(null);
        store.save(PushChannelLog.builder()
                .userId(1L)
                .channel("appPush")
                .bizType("approval")
                .bizId(100L)
                .success(1)
                .build());
        store.save(PushChannelLog.builder()
                .userId(2L)
                .channel("mpSubscribe")
                .bizType("reminder")
                .success(0)
                .reason("token expired")
                .build());
        // 无断言失败即视为通过
    }
}
