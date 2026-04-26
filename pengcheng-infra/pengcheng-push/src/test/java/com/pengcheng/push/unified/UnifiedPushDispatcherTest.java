package com.pengcheng.push.unified;

import com.pengcheng.push.PushService;
import com.pengcheng.push.PushServiceFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedPushDispatcherTest {

    @Test
    void decisionRule_picksAppWhenOnline() {
        PushTarget target = PushTarget.builder()
                .userId("u1")
                .registrationId("reg-1")
                .appOnline(true)
                .miniProgramOpenId("openid-1")
                .miniProgramSubscribed(true)
                .webInboxEnabled(true)
                .build();
        assertEquals(PushChannel.APP_PUSH, PushDecisionRule.decide(target));
    }

    @Test
    void decisionRule_picksAppEvenOffline_inDefaultMode() {
        PushTarget target = PushTarget.builder()
                .userId("u1")
                .registrationId("reg-1")
                .appOnline(false)
                .miniProgramOpenId("openid-1")
                .miniProgramSubscribed(true)
                .build();
        assertEquals(PushChannel.APP_PUSH, PushDecisionRule.decide(target));
    }

    @Test
    void decisionRule_strict_skipsOfflineAppForCriticalMessages() {
        PushTarget target = PushTarget.builder()
                .userId("u1")
                .registrationId("reg-1")
                .appOnline(false)
                .miniProgramOpenId("openid-1")
                .miniProgramSubscribed(true)
                .webInboxEnabled(true)
                .build();
        assertEquals(PushChannel.MP_SUBSCRIBE, PushDecisionRule.decideStrict(target));
    }

    @Test
    void decisionRule_picksSubscribeWhenNoApp() {
        PushTarget target = PushTarget.builder()
                .userId("u1")
                .registrationId(null)
                .miniProgramOpenId("openid-1")
                .miniProgramSubscribed(true)
                .webInboxEnabled(true)
                .build();
        assertEquals(PushChannel.MP_SUBSCRIBE, PushDecisionRule.decide(target));
    }

    @Test
    void decisionRule_fallbackToInbox() {
        PushTarget target = PushTarget.builder()
                .userId("u1")
                .registrationId("")
                .miniProgramOpenId(null)
                .webInboxEnabled(true)
                .build();
        assertEquals(PushChannel.WEB_INBOX, PushDecisionRule.decide(target));
    }

    @Test
    void decisionRule_noneWhenAllUnavailable() {
        PushTarget target = PushTarget.builder()
                .userId("u1")
                .registrationId(null)
                .miniProgramOpenId(null)
                .miniProgramSubscribed(false)
                .webInboxEnabled(false)
                .build();
        assertEquals(PushChannel.NONE, PushDecisionRule.decide(target));
        assertEquals(PushChannel.NONE, PushDecisionRule.decideStrict(target));
        assertEquals(PushChannel.NONE, PushDecisionRule.decide(null));
    }

    @Test
    void dispatcher_appOnline_sendsViaAppPush() {
        StubPushFactory factory = new StubPushFactory(true);
        StubSubscribeSender subscribe = new StubSubscribeSender(true);
        StubInboxSender inbox = new StubInboxSender(true);
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(factory, subscribe, inbox);

        PushTarget target = PushTarget.builder()
                .userId("u-1").registrationId("reg").appOnline(true).build();
        PushPayload payload = PushPayload.builder().title("t").content("c").bizType("approval").bizId(99L).build();

        PushDispatchResult res = dispatcher.dispatch(target, payload);

        assertTrue(res.isSuccess());
        assertEquals(PushChannel.APP_PUSH, res.getChannel());
        assertEquals(1, factory.getStub().pushed.size());
        assertEquals(0, subscribe.calls);
        assertEquals(0, inbox.calls);
    }

    @Test
    void dispatcher_appOffline_noOpenId_fallsBackToInbox() {
        StubPushFactory factory = new StubPushFactory(false);
        StubSubscribeSender subscribe = new StubSubscribeSender(true);
        StubInboxSender inbox = new StubInboxSender(true);
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(factory, subscribe, inbox);

        PushTarget target = PushTarget.builder()
                .userId("u-2").registrationId(null).webInboxEnabled(true).build();
        PushPayload payload = PushPayload.builder().title("t").content("c").bizType("customer").bizId(7L).build();

        PushDispatchResult res = dispatcher.dispatch(target, payload);
        assertTrue(res.isSuccess());
        assertEquals(PushChannel.WEB_INBOX, res.getChannel());
        assertEquals(1, inbox.calls);
    }

    @Test
    void dispatcher_subscribeWithoutTemplate_returnsFail() {
        StubPushFactory factory = new StubPushFactory(true);
        StubSubscribeSender subscribe = new StubSubscribeSender(true);
        StubInboxSender inbox = new StubInboxSender(true);
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(factory, subscribe, inbox, false);

        PushTarget target = PushTarget.builder()
                .userId("u-3").registrationId(null).miniProgramOpenId("op").miniProgramSubscribed(true)
                .webInboxEnabled(true).build();
        // 故意不设置 subscribeTemplateId
        PushPayload payload = PushPayload.builder().title("t").content("c").build();

        PushDispatchResult res = dispatcher.dispatch(target, payload);
        assertFalse(res.isSuccess());
        assertEquals(PushChannel.MP_SUBSCRIBE, res.getChannel());
    }

    @Test
    void dispatcher_appFails_fallsBackToSubscribe() {
        StubPushFactory factory = new StubPushFactory(false);
        StubSubscribeSender subscribe = new StubSubscribeSender(true);
        StubInboxSender inbox = new StubInboxSender(true);
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(factory, subscribe, inbox);

        PushTarget target = PushTarget.builder()
                .userId("u-4").registrationId("reg").appOnline(true)
                .miniProgramOpenId("op").miniProgramSubscribed(true).webInboxEnabled(true).build();
        PushPayload payload = PushPayload.builder()
                .title("t").content("c")
                .subscribeTemplateId("tpl-1")
                .subscribeData(Map.of("name", "Alice"))
                .build();

        PushDispatchResult res = dispatcher.dispatch(target, payload);
        assertTrue(res.isSuccess());
        assertEquals(PushChannel.MP_SUBSCRIBE, res.getChannel());
    }

    @Test
    void dispatcher_nullArgs_returnsFail() {
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(null, null, null);
        PushDispatchResult res = dispatcher.dispatch(null, null);
        assertFalse(res.isSuccess());
        assertEquals(PushChannel.NONE, res.getChannel());
    }

    @Test
    void dispatcher_runtimeException_isCaughtAndReported() {
        ChannelInboxSender boom = (uid, t, c, b, id) -> {
            throw new IllegalStateException("oops");
        };
        UnifiedPushDispatcher dispatcher = new UnifiedPushDispatcher(null, null, boom, false);

        PushTarget target = PushTarget.builder().userId("u").webInboxEnabled(true).build();
        PushPayload payload = PushPayload.builder().title("t").content("c").build();

        PushDispatchResult res = dispatcher.dispatch(target, payload);
        assertFalse(res.isSuccess());
        assertEquals(PushChannel.WEB_INBOX, res.getChannel());
        assertTrue(res.getReason().contains("oops"));
    }

    @Test
    void payloadEnumDisplay() {
        assertEquals("APP 推送", PushChannel.APP_PUSH.getDisplayName());
        assertEquals("appPush", PushChannel.APP_PUSH.getCode());
        assertNull(PushDispatchResult.ok(PushChannel.WEB_INBOX).getReason());
    }

    // -------- helpers --------

    /** PushServiceFactory 的可控 stub：不读取 SystemConfigHelper */
    static class StubPushFactory extends PushServiceFactory {
        private final StubPushService stub;

        StubPushFactory(boolean returnSuccess) {
            super(null);
            this.stub = new StubPushService(returnSuccess);
        }

        @Override
        public PushService getPushService() {
            return stub;
        }

        StubPushService getStub() {
            return stub;
        }
    }

    static class StubPushService implements PushService {
        final boolean ok;
        final List<String> pushed = new java.util.ArrayList<>();

        StubPushService(boolean ok) {
            this.ok = ok;
        }

        @Override public String getProviderType() { return "stub"; }
        @Override public String getProviderName() { return "Stub"; }
        @Override public boolean pushToUser(String u, String t, String c, Map<String, String> e) {
            pushed.add(u + ":" + t);
            return ok;
        }
        @Override public boolean pushToUsers(List<String> u, String t, String c, Map<String, String> e) { return ok; }
        @Override public boolean pushToAll(String t, String c, Map<String, String> e) { return ok; }
        @Override public boolean pushToTags(List<String> tags, String t, String c, Map<String, String> e) { return ok; }
        @Override public boolean pushToDevice(String r, String t, String c, Map<String, String> e) { return ok; }
    }

    static class StubSubscribeSender implements ChannelSubscribeSender {
        final boolean ok;
        int calls = 0;
        Map<String, String> lastData = new HashMap<>();

        StubSubscribeSender(boolean ok) { this.ok = ok; }

        @Override
        public boolean send(String openId, String templateId, Map<String, String> data, String page) {
            calls++;
            if (data != null) lastData = data;
            return ok;
        }
    }

    static class StubInboxSender implements ChannelInboxSender {
        final boolean ok;
        int calls = 0;

        StubInboxSender(boolean ok) { this.ok = ok; }

        @Override
        public boolean send(String userId, String title, String content, String bizType, Long bizId) {
            calls++;
            return ok;
        }
    }
}
