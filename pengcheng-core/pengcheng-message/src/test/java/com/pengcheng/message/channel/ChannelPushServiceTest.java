package com.pengcheng.message.channel;

import com.pengcheng.push.unified.ChannelInboxSender;
import com.pengcheng.push.unified.ChannelSubscribeSender;
import com.pengcheng.push.unified.PushChannel;
import com.pengcheng.push.unified.PushDispatchResult;
import com.pengcheng.push.unified.UnifiedPushDispatcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelPushServiceTest {

    @Test
    void appOnline_pushesViaApp_andLogsSuccess() {
        StubResolver resolver = new StubResolver(UserChannelProfile.builder()
                .userId(1L)
                .appRegistrationId("reg-1")
                .appOnline(true)
                .miniProgramOpenId("op")
                .miniProgramSubscribed(true)
                .webInboxEnabled(true)
                .build());
        StubInbox inbox = new StubInbox(true);
        StubSubscribe subscribe = new StubSubscribe(true);
        StubAppPush appPush = new StubAppPush(true);
        StubLogStore logStore = new StubLogStore();
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(
                appPush.asFactory(), subscribe, inbox);
        ChannelPushService service = new ChannelPushService(resolver, dispatcher, logStore);

        PushDispatchResult res = service.push(1L, ChannelPushRequest.builder()
                .title("审批通知").content("新审批待处理")
                .bizType("approval").bizId(99L).build());

        assertEquals(PushChannel.APP_PUSH, res.getChannel());
        assertTrue(res.isSuccess());
        assertEquals(1, logStore.records.size());
        assertEquals("appPush", logStore.records.get(0).getChannel());
        assertEquals(1, logStore.records.get(0).getSuccess());
    }

    @Test
    void offlineAndNoSubscribe_fallsBackToInbox() {
        StubResolver resolver = new StubResolver(UserChannelProfile.builder()
                .userId(2L)
                .appRegistrationId(null)
                .miniProgramOpenId(null)
                .webInboxEnabled(true)
                .build());
        StubInbox inbox = new StubInbox(true);
        StubSubscribe subscribe = new StubSubscribe(true);
        StubAppPush appPush = new StubAppPush(false);
        StubLogStore logStore = new StubLogStore();
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(
                appPush.asFactory(), subscribe, inbox);
        ChannelPushService service = new ChannelPushService(resolver, dispatcher, logStore);

        PushDispatchResult res = service.push(2L, ChannelPushRequest.builder()
                .title("客户提醒").content("客户跟进")
                .bizType("customer").bizId(5L).build());

        assertEquals(PushChannel.WEB_INBOX, res.getChannel());
        assertTrue(res.isSuccess());
        assertEquals(1, inbox.calls);
        assertEquals(0, subscribe.calls);
        assertEquals("webInbox", logStore.records.get(0).getChannel());
    }

    @Test
    void noAppButHasSubscribe_picksMpSubscribe() {
        StubResolver resolver = new StubResolver(UserChannelProfile.builder()
                .userId(3L)
                .appRegistrationId(null)
                .miniProgramOpenId("openid-x")
                .miniProgramSubscribed(true)
                .webInboxEnabled(true)
                .build());
        StubInbox inbox = new StubInbox(true);
        StubSubscribe subscribe = new StubSubscribe(true);
        StubAppPush appPush = new StubAppPush(true);
        StubLogStore logStore = new StubLogStore();
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(
                appPush.asFactory(), subscribe, inbox);
        ChannelPushService service = new ChannelPushService(resolver, dispatcher, logStore);

        PushDispatchResult res = service.push(3L, ChannelPushRequest.builder()
                .title("审批通知").content("已通过")
                .bizType("approval").bizId(77L)
                .subscribeTemplateId("tpl-approval")
                .subscribeData(Map.of("status", "已通过"))
                .build());

        assertEquals(PushChannel.MP_SUBSCRIBE, res.getChannel());
        assertTrue(res.isSuccess());
        assertEquals(1, subscribe.calls);
    }

    @Test
    void resolverReturnsNull_fallsBackToInboxOnly() {
        StubResolver resolver = new StubResolver(null);
        StubInbox inbox = new StubInbox(true);
        StubSubscribe subscribe = new StubSubscribe(true);
        StubAppPush appPush = new StubAppPush(false);
        StubLogStore logStore = new StubLogStore();
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(
                appPush.asFactory(), subscribe, inbox);
        ChannelPushService service = new ChannelPushService(resolver, dispatcher, logStore);

        PushDispatchResult res = service.push(4L, ChannelPushRequest.builder()
                .bizType("approval").bizId(1L).title("t").content("c").build());

        assertEquals(PushChannel.WEB_INBOX, res.getChannel());
        assertTrue(res.isSuccess());
    }

    @Test
    void nullArgs_returnsNoneFail() {
        ChannelPushService service = new ChannelPushService(
                new StubResolver(null), new UnifiedPushDispatcher(null, null, null), new StubLogStore());
        PushDispatchResult res = service.push(null, null);
        assertFalse(res.isSuccess());
        assertEquals(PushChannel.NONE, res.getChannel());
    }

    @Test
    void logStoreException_doesNotBreakBusiness() {
        StubResolver resolver = new StubResolver(UserChannelProfile.builder()
                .userId(5L).appRegistrationId(null).webInboxEnabled(true).build());
        StubInbox inbox = new StubInbox(true);
        StubAppPush appPush = new StubAppPush(false);
        PushChannelLogStore boomStore = log -> {
            throw new IllegalStateException("db down");
        };
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(
                appPush.asFactory(), null, inbox);
        ChannelPushService service = new ChannelPushService(resolver, dispatcher, boomStore);

        PushDispatchResult res = service.push(5L,
                ChannelPushRequest.builder().title("t").content("c").bizType("x").bizId(0L).build());
        assertTrue(res.isSuccess());
    }

    // -------- helpers --------

    static class StubResolver implements UserChannelResolver {
        final UserChannelProfile profile;
        StubResolver(UserChannelProfile profile) { this.profile = profile; }
        @Override public UserChannelProfile resolve(Long userId) { return profile; }
    }

    static class StubInbox implements ChannelInboxSender {
        final boolean ok;
        int calls = 0;
        StubInbox(boolean ok) { this.ok = ok; }
        @Override public boolean send(String userId, String title, String content,
                                      String bizType, Long bizId) {
            calls++;
            return ok;
        }
    }

    static class StubSubscribe implements ChannelSubscribeSender {
        final boolean ok;
        int calls = 0;
        StubSubscribe(boolean ok) { this.ok = ok; }
        @Override public boolean send(String openId, String tpl, Map<String, String> data, String page) {
            calls++;
            return ok;
        }
    }

    static class StubAppPush {
        final boolean ok;
        StubAppPush(boolean ok) { this.ok = ok; }
        com.pengcheng.push.PushServiceFactory asFactory() {
            return new com.pengcheng.push.PushServiceFactory(null) {
                @Override
                public com.pengcheng.push.PushService getPushService() {
                    return new com.pengcheng.push.PushService() {
                        @Override public String getProviderType() { return "stub"; }
                        @Override public String getProviderName() { return "Stub"; }
                        @Override public boolean pushToUser(String u, String t, String c, Map<String,String> e) { return ok; }
                        @Override public boolean pushToUsers(List<String> u, String t, String c, Map<String,String> e) { return ok; }
                        @Override public boolean pushToAll(String t, String c, Map<String,String> e) { return ok; }
                        @Override public boolean pushToTags(List<String> t1, String t, String c, Map<String,String> e) { return ok; }
                        @Override public boolean pushToDevice(String r, String t, String c, Map<String,String> e) { return ok; }
                    };
                }
            };
        }
    }

    static class StubLogStore implements PushChannelLogStore {
        final List<PushChannelLog> records = new ArrayList<>();
        @Override public void save(PushChannelLog log) { records.add(log); }
    }
}
