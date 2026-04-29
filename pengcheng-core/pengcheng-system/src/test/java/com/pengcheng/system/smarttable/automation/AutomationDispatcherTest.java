package com.pengcheng.system.smarttable.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.smarttable.automation.action.AutomationAction;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationLog;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationRule;
import com.pengcheng.system.smarttable.automation.service.AutomationLogService;
import com.pengcheng.system.smarttable.automation.service.AutomationRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutomationDispatcher — 触发派发逻辑")
class AutomationDispatcherTest {

    @Mock
    private AutomationRuleService ruleService;
    @Mock
    private AutomationLogService logService;
    @Mock
    private AutomationAction mockAction;

    private AutomationDispatcher dispatcher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(mockAction.type()).thenReturn("CREATE_TODO");
        ConditionEvaluator evaluator = new ConditionEvaluator(objectMapper);
        Map<String, AutomationAction> actionBeanMap = Map.of("createTodoAction", mockAction);
        dispatcher = new AutomationDispatcher(ruleService, logService, evaluator, objectMapper, actionBeanMap);
    }

    private SmartTableAutomationRule makeRule(String triggerType, String conditionJson, String actionsJson) {
        SmartTableAutomationRule rule = new SmartTableAutomationRule();
        rule.setId(1L);
        rule.setTableId(10L);
        rule.setName("测试规则");
        rule.setEnabled(1);
        rule.setTriggerType(triggerType);
        rule.setConditionJson(conditionJson);
        rule.setActionsJson(actionsJson);
        return rule;
    }

    private AutomationEvent makeEvent(AutomationTriggerType type, Map<String, Object> newRow) {
        return AutomationEvent.builder()
                .tableId(10L)
                .recordId(100L)
                .triggerType(type)
                .newRow(newRow)
                .build();
    }

    @Test
    @DisplayName("触发匹配：规则 triggerType 匹配且条件满足时执行 action")
    void dispatch_matchingTrigger_executesAction() throws Exception {
        String actionsJson = """
                [{"type":"CREATE_TODO","params":{"title":"Test"}}]
                """;
        SmartTableAutomationRule rule = makeRule("RECORD_CREATED", null, actionsJson);
        when(ruleService.listByTrigger(10L, "RECORD_CREATED")).thenReturn(List.of(rule));

        dispatcher.dispatch(makeEvent(AutomationTriggerType.RECORD_CREATED, Map.of("name", "v")));

        verify(mockAction).execute(any(), any());
        verify(logService).save(any());
    }

    @Test
    @DisplayName("条件不满足：condition_json 不通过时跳过 action")
    void dispatch_conditionFails_skipsAction() throws Exception {
        String cond = """
                {"op":"EQ","field":"status","value":"done"}
                """;
        String actionsJson = """
                [{"type":"CREATE_TODO","params":{}}]
                """;
        SmartTableAutomationRule rule = makeRule("RECORD_UPDATED", cond, actionsJson);
        when(ruleService.listByTrigger(10L, "RECORD_UPDATED")).thenReturn(List.of(rule));

        dispatcher.dispatch(makeEvent(AutomationTriggerType.RECORD_UPDATED, Map.of("status", "pending")));

        verify(mockAction, never()).execute(any(), any());
        verify(logService, never()).save(any());
    }

    @Test
    @DisplayName("多 action 串行：两个 action 均应被调用")
    void dispatch_multipleActions_bothExecuted() throws Exception {
        AutomationAction mockAction2 = mock(AutomationAction.class);
        when(mockAction2.type()).thenReturn("SEND_EMAIL");

        Map<String, AutomationAction> actionBeanMap = Map.of(
                "createTodoAction", mockAction,
                "sendEmailAction", mockAction2
        );
        ConditionEvaluator evaluator = new ConditionEvaluator(objectMapper);
        dispatcher = new AutomationDispatcher(ruleService, logService, evaluator, objectMapper, actionBeanMap);

        String actionsJson = """
                [
                  {"type":"CREATE_TODO","params":{"title":"T1"}},
                  {"type":"SEND_EMAIL","params":{"to":"a@b.com"}}
                ]
                """;
        SmartTableAutomationRule rule = makeRule("RECORD_CREATED", null, actionsJson);
        when(ruleService.listByTrigger(10L, "RECORD_CREATED")).thenReturn(List.of(rule));

        dispatcher.dispatch(makeEvent(AutomationTriggerType.RECORD_CREATED, Map.of()));

        verify(mockAction).execute(any(), any());
        verify(mockAction2).execute(any(), any());
    }

    @Test
    @DisplayName("异常隔离：第一个 action 抛异常不影响第二个 action 执行")
    void dispatch_firstActionFails_secondStillExecuted() throws Exception {
        AutomationAction failAction = mock(AutomationAction.class);
        when(failAction.type()).thenReturn("FAIL_ACTION");
        doThrow(new RuntimeException("模拟异常")).when(failAction).execute(any(), any());

        Map<String, AutomationAction> actionBeanMap = Map.of(
                "failAction", failAction,
                "createTodoAction", mockAction
        );
        ConditionEvaluator evaluator = new ConditionEvaluator(objectMapper);
        dispatcher = new AutomationDispatcher(ruleService, logService, evaluator, objectMapper, actionBeanMap);

        String actionsJson = """
                [
                  {"type":"FAIL_ACTION","params":{}},
                  {"type":"CREATE_TODO","params":{"title":"T"}}
                ]
                """;
        SmartTableAutomationRule rule = makeRule("RECORD_CREATED", null, actionsJson);
        when(ruleService.listByTrigger(10L, "RECORD_CREATED")).thenReturn(List.of(rule));

        dispatcher.dispatch(makeEvent(AutomationTriggerType.RECORD_CREATED, Map.of()));

        // 第一个失败但第二个仍然执行
        verify(mockAction).execute(any(), any());
        // 日志应记录部分失败（success=0）
        ArgumentCaptor<SmartTableAutomationLog> logCaptor = ArgumentCaptor.forClass(SmartTableAutomationLog.class);
        verify(logService).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isEqualTo(0);
        assertThat(logCaptor.getValue().getErrorMsg()).contains("模拟异常");
    }

    @Test
    @DisplayName("dry-run：不实际调用 action，写日志标记 success=1")
    void dispatch_dryRun_noRealActionExecution() throws Exception {
        String actionsJson = """
                [{"type":"CREATE_TODO","params":{"title":"T"}}]
                """;
        SmartTableAutomationRule rule = makeRule("RECORD_CREATED", null, actionsJson);
        when(ruleService.listByTrigger(10L, "RECORD_CREATED")).thenReturn(List.of(rule));

        AutomationEvent event = AutomationEvent.builder()
                .tableId(10L)
                .recordId(100L)
                .triggerType(AutomationTriggerType.RECORD_CREATED)
                .newRow(Map.of())
                .dryRun(true)
                .build();

        dispatcher.dispatch(event);

        verify(mockAction, never()).execute(any(), any());
        ArgumentCaptor<SmartTableAutomationLog> logCaptor = ArgumentCaptor.forClass(SmartTableAutomationLog.class);
        verify(logService).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isEqualTo(1);
    }

    @Test
    @DisplayName("日志写入：正常触发后写入一条 success=1 的日志")
    void dispatch_successFlow_writesLog() throws Exception {
        String actionsJson = """
                [{"type":"CREATE_TODO","params":{}}]
                """;
        SmartTableAutomationRule rule = makeRule("RECORD_DELETED", null, actionsJson);
        when(ruleService.listByTrigger(10L, "RECORD_DELETED")).thenReturn(List.of(rule));

        dispatcher.dispatch(makeEvent(AutomationTriggerType.RECORD_DELETED, null));

        ArgumentCaptor<SmartTableAutomationLog> captor = ArgumentCaptor.forClass(SmartTableAutomationLog.class);
        verify(logService).save(captor.capture());
        SmartTableAutomationLog log = captor.getValue();
        assertThat(log.getRuleId()).isEqualTo(1L);
        assertThat(log.getTableId()).isEqualTo(10L);
        assertThat(log.getTriggerType()).isEqualTo("RECORD_DELETED");
        assertThat(log.getSuccess()).isEqualTo(1);
    }
}
