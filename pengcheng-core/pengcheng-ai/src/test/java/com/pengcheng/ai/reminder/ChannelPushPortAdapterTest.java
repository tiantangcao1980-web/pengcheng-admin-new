package com.pengcheng.ai.reminder;

import com.pengcheng.ai.reminder.entity.AiReminderRule;
import com.pengcheng.system.channel.service.ChannelPushService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChannelPushPortAdapterTest {

    @Test
    void push_shouldDelegateToChannelPushService() {
        ChannelPushService service = mock(ChannelPushService.class);
        ChannelPushPortAdapter adapter = new ChannelPushPortAdapter(service);

        AiReminderRule rule = new AiReminderRule();
        rule.setRuleCode("DAILY_FOLLOWUP");
        rule.setRuleName("每日待跟进");
        ReminderTarget target = ReminderTarget.builder()
                .userId(1L)
                .title("提醒")
                .content("有 3 个客户待跟进")
                .build();

        boolean ok = adapter.push(rule, target);

        assertThat(ok).isTrue();
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        verify(service, times(1)).broadcast(anyString(), anyString(), typeCap.capture());
        assertThat(typeCap.getValue()).isEqualTo("ai_reminder:DAILY_FOLLOWUP");
    }

    @Test
    void push_shouldReturnFalseWhenContentBlank() {
        ChannelPushService service = mock(ChannelPushService.class);
        ChannelPushPortAdapter adapter = new ChannelPushPortAdapter(service);

        boolean ok = adapter.push(new AiReminderRule(),
                ReminderTarget.builder().userId(1L).content("  ").build());

        assertThat(ok).isFalse();
        verify(service, never()).broadcast(any(), any(), any());
    }

    @Test
    void push_shouldSwallowExceptionAndReturnFalse() {
        ChannelPushService service = mock(ChannelPushService.class);
        doThrow(new RuntimeException("network down"))
                .when(service).broadcast(any(), any(), any());

        ChannelPushPortAdapter adapter = new ChannelPushPortAdapter(service);
        AiReminderRule rule = new AiReminderRule();
        rule.setRuleCode("X");
        ReminderTarget target = ReminderTarget.builder()
                .userId(1L).title("t").content("c").build();

        boolean ok = adapter.push(rule, target);
        assertThat(ok).isFalse();
    }
}
