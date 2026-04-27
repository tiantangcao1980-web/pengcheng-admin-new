package com.pengcheng.system.smarttable.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.mail.EmailService;
import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.system.smarttable.automation.action.*;
import com.pengcheng.system.smarttable.entity.SmartTableRecord;
import com.pengcheng.system.smarttable.mapper.SmartTableRecordMapper;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.service.TodoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("各内置 Action 单元测试")
class ActionUnitTest {

    private AutomationEvent baseEvent() {
        return AutomationEvent.builder()
                .tableId(10L)
                .recordId(100L)
                .triggerType(AutomationTriggerType.RECORD_CREATED)
                .newRow(Map.of("name", "Alice", "status", "done"))
                .build();
    }

    // ==================== CreateTodoAction ====================

    @Mock
    private TodoService todoService;

    @Test
    @DisplayName("CreateTodoAction：创建待办，title 支持 {{fieldKey}} 占位符替换")
    void createTodoAction_createsWithRenderedTitle() {
        CreateTodoAction action = new CreateTodoAction(todoService);
        when(todoService.createTodo(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> params = Map.of("title", "处理{{name}}的申请", "priority", 1);
        action.execute(params, baseEvent());

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoService).createTodo(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("处理Alice的申请");
        assertThat(captor.getValue().getPriority()).isEqualTo(1);
    }

    // ==================== SendEmailAction ====================

    @Mock
    private EmailService emailService;

    @Test
    @DisplayName("SendEmailAction：缺少 to 参数时抛出 IllegalArgumentException")
    void sendEmailAction_missingTo_throws() {
        SendEmailAction action = new SendEmailAction(emailService);
        Map<String, Object> params = Map.of("subject", "Test");
        assertThatThrownBy(() -> action.execute(params, baseEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");
    }

    // ==================== UpdateRecordAction ====================

    @Mock
    private SmartTableRecordMapper recordMapper;

    @Test
    @DisplayName("UpdateRecordAction：合并 fields 到 record.data 并保存")
    void updateRecordAction_mergesFields() throws Exception {
        UpdateRecordAction action = new UpdateRecordAction(recordMapper);

        SmartTableRecord record = new SmartTableRecord();
        record.setId(100L);
        record.setData(new HashMap<>(Map.of("name", "Alice", "status", "pending")));
        when(recordMapper.selectById(100L)).thenReturn(record);

        Map<String, Object> params = Map.of("fields", Map.of("status", "done", "remark", "auto"));
        action.execute(params, baseEvent());

        verify(recordMapper).updateById(record);
        assertThat(record.getData().get("status")).isEqualTo("done");
        assertThat(record.getData().get("name")).isEqualTo("Alice"); // 原有字段保留
    }

    // ==================== CallWebhookAction ====================

    @Test
    @DisplayName("CallWebhookAction：缺少 url 参数时抛出 IllegalArgumentException")
    void callWebhookAction_missingUrl_throws() {
        CallWebhookAction action = new CallWebhookAction(new ObjectMapper());
        Map<String, Object> params = Map.of("extraBody", Map.of());
        assertThatThrownBy(() -> action.execute(params, baseEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
    }

    // ==================== SendNotificationAction ====================

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("SendNotificationAction：创建通知，userId 正确映射")
    void sendNotificationAction_createsNotification() {
        SendNotificationAction action = new SendNotificationAction(notificationService);
        Map<String, Object> params = Map.of("userId", 999L, "title", "通知{{name}}", "content", "内容");
        action.execute(params, baseEvent());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).createNotification(captor.capture());
        Notification n = captor.getValue();
        assertThat(n.getUserId()).isEqualTo(999L);
        assertThat(n.getTitle()).isEqualTo("通知Alice");
        assertThat(n.getReadStatus()).isEqualTo(0);
    }
}
