package com.pengcheng.message.subscribe;

import com.pengcheng.push.unified.ChannelSubscribeSender;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscribeMessageServiceTest {

    @Test
    void renderer_appliesFieldMapping() {
        SubscribeMsgTemplate tpl = SubscribeMsgTemplate.builder()
                .templateId("tpl-1")
                .fieldMappingJson("{\"applicantName\":\"thing1\",\"status\":\"phrase2\"}")
                .build();
        Map<String, String> biz = new HashMap<>();
        biz.put("applicantName", "张三");
        biz.put("status", "已通过");
        biz.put("ignored", "x");
        Map<String, String> rendered = SubscribeMessageRenderer.render(tpl, biz);
        assertEquals("张三", rendered.get("thing1"));
        assertEquals("已通过", rendered.get("phrase2"));
        assertFalse(rendered.containsKey("ignored"));
    }

    @Test
    void renderer_missingBizValue_isBlankString() {
        SubscribeMsgTemplate tpl = SubscribeMsgTemplate.builder()
                .templateId("tpl-1")
                .fieldMappingJson("{\"applicantName\":\"thing1\"}")
                .build();
        Map<String, String> rendered = SubscribeMessageRenderer.render(tpl, new HashMap<>());
        assertEquals("", rendered.get("thing1"));
    }

    @Test
    void renderer_invalidJson_fallsBackToPassthrough() {
        SubscribeMsgTemplate tpl = SubscribeMsgTemplate.builder()
                .templateId("tpl-1")
                .fieldMappingJson("not-a-json")
                .build();
        Map<String, String> biz = Map.of("k", "v");
        Map<String, String> rendered = SubscribeMessageRenderer.render(tpl, biz);
        assertEquals("v", rendered.get("k"));
    }

    @Test
    void renderer_nullTemplate_passthrough() {
        Map<String, String> rendered = SubscribeMessageRenderer.render(null, Map.of("k", "v"));
        assertEquals("v", rendered.get("k"));
    }

    @Test
    void renderer_blankMappingJson_passthrough() {
        SubscribeMsgTemplate tpl = SubscribeMsgTemplate.builder()
                .templateId("tpl-1").fieldMappingJson("").build();
        Map<String, String> rendered = SubscribeMessageRenderer.render(tpl, Map.of("a", "b"));
        assertEquals("b", rendered.get("a"));
    }

    @Test
    void service_send_happyPath() {
        SubscribeMsgTemplate tpl = SubscribeMsgTemplate.builder()
                .templateId("wx-tpl-1")
                .fieldMappingJson("{\"name\":\"thing1\"}")
                .defaultPage("/pages/index")
                .enabled(1)
                .build();
        StubRepo repo = new StubRepo(tpl);
        StubSender sender = new StubSender(true);
        SubscribeMessageService service = new SubscribeMessageService(repo, sender);

        boolean ok = service.send(SubscribeMessageRequest.builder()
                .openId("open-1").bizType("approval").eventCode("approved")
                .bizFields(Map.of("name", "Alice")).build());
        assertTrue(ok);
        assertEquals("wx-tpl-1", sender.lastTemplateId);
        assertEquals("/pages/index", sender.lastPage);
        assertEquals("Alice", sender.lastData.get("thing1"));
    }

    @Test
    void service_send_pageOverride() {
        StubRepo repo = new StubRepo(SubscribeMsgTemplate.builder()
                .templateId("tpl").defaultPage("/pages/default").build());
        StubSender sender = new StubSender(true);
        SubscribeMessageService service = new SubscribeMessageService(repo, sender);
        service.send(SubscribeMessageRequest.builder()
                .openId("op").bizType("x").eventCode("y").page("/pages/custom").build());
        assertEquals("/pages/custom", sender.lastPage);
    }

    @Test
    void service_send_missingOpenId_returnsFalse() {
        SubscribeMessageService service = new SubscribeMessageService(new StubRepo(null), new StubSender(true));
        assertFalse(service.send(SubscribeMessageRequest.builder().bizType("x").eventCode("y").build()));
        assertFalse(service.send(SubscribeMessageRequest.builder().openId("").bizType("x").eventCode("y").build()));
        assertFalse(service.send(null));
    }

    @Test
    void service_send_templateNotFound_returnsFalse() {
        SubscribeMessageService service = new SubscribeMessageService(new StubRepo(null), new StubSender(true));
        assertFalse(service.send(SubscribeMessageRequest.builder()
                .openId("op").bizType("x").eventCode("y").build()));
    }

    @Test
    void service_send_senderFails_propagatesFalse() {
        StubRepo repo = new StubRepo(SubscribeMsgTemplate.builder().templateId("tpl").build());
        StubSender sender = new StubSender(false);
        SubscribeMessageService service = new SubscribeMessageService(repo, sender);
        assertFalse(service.send(SubscribeMessageRequest.builder()
                .openId("op").bizType("x").eventCode("y").build()));
    }

    static class StubRepo implements SubscribeMsgTemplateRepository {
        final SubscribeMsgTemplate tpl;
        StubRepo(SubscribeMsgTemplate tpl) { this.tpl = tpl; }
        @Override public Optional<SubscribeMsgTemplate> findEnabled(String bizType, String eventCode) {
            return Optional.ofNullable(tpl);
        }
    }

    static class StubSender implements ChannelSubscribeSender {
        final boolean ok;
        String lastTemplateId;
        String lastPage;
        Map<String, String> lastData;
        StubSender(boolean ok) { this.ok = ok; }
        @Override public boolean send(String openId, String templateId, Map<String, String> data, String page) {
            this.lastTemplateId = templateId;
            this.lastPage = page;
            this.lastData = data;
            return ok;
        }
    }
}
