package com.pengcheng.system.smarttable.automation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.smarttable.automation.action.AutomationAction;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationLog;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationRule;
import com.pengcheng.system.smarttable.automation.service.AutomationLogService;
import com.pengcheng.system.smarttable.automation.service.AutomationRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自动化事件派发器
 *
 * <p>业务方在写记录后手动调用 {@link #dispatch(AutomationEvent)} 即可触发自动化。
 *
 * <p>核心流程：
 * <ol>
 *   <li>按 tableId + triggerType 查找已启用规则</li>
 *   <li>对 FIELD_CHANGED 规则额外比对 trigger_config.fieldKey</li>
 *   <li>评估 condition_json（空条件视为满足）</li>
 *   <li>按顺序执行 actions_json 中每个 action</li>
 *   <li><b>异常隔离</b>：单个 action 失败仅记录错误，不影响后续 action；最终写一条汇总日志</li>
 *   <li><b>dry-run</b>：{@code event.dryRun=true} 时只评估条件 + 解析 action，不真正执行副作用</li>
 * </ol>
 */
@Slf4j
@Component
public class AutomationDispatcher {

    private final AutomationRuleService ruleService;
    private final AutomationLogService logService;
    private final ConditionEvaluator conditionEvaluator;
    private final ObjectMapper objectMapper;
    /** key = action.type()，Spring 自动按 Bean name 注入 Map */
    private final Map<String, AutomationAction> actionMap;

    public AutomationDispatcher(AutomationRuleService ruleService,
                                AutomationLogService logService,
                                ConditionEvaluator conditionEvaluator,
                                ObjectMapper objectMapper,
                                Map<String, AutomationAction> actionMap) {
        this.ruleService = ruleService;
        this.logService = logService;
        this.conditionEvaluator = conditionEvaluator;
        this.objectMapper = objectMapper;
        // 将 Spring Bean name → action 转换为 type() → action 索引
        java.util.Map<String, AutomationAction> byType = new java.util.HashMap<>();
        actionMap.values().forEach(a -> byType.put(a.type(), a));
        this.actionMap = byType;
    }

    /**
     * 分发自动化事件（同步执行）
     *
     * @param event 触发事件
     */
    public void dispatch(AutomationEvent event) {
        if (event == null || event.getTableId() == null) return;

        List<SmartTableAutomationRule> rules = ruleService.listByTrigger(
                event.getTableId(), event.getTriggerType().name());

        for (SmartTableAutomationRule rule : rules) {
            processRule(rule, event);
        }
    }

    private void processRule(SmartTableAutomationRule rule, AutomationEvent event) {
        // FIELD_CHANGED：校验 fieldKey 是否匹配
        if (AutomationTriggerType.FIELD_CHANGED == event.getTriggerType()) {
            String configuredField = extractFieldKey(rule.getTriggerConfig());
            if (configuredField != null && !configuredField.equals(event.getFieldKey())) {
                return;
            }
        }

        // 评估条件
        Map<String, Object> contextRow = event.getNewRow() != null ? event.getNewRow() : event.getOldRow();
        if (!conditionEvaluator.evaluate(rule.getConditionJson(), contextRow)) {
            log.debug("[Automation] 规则 {} 条件不满足，跳过", rule.getId());
            return;
        }

        // 解析 actions
        List<Map<String, Object>> actionDefs = parseActions(rule.getActionsJson());
        if (actionDefs == null || actionDefs.isEmpty()) {
            log.warn("[Automation] 规则 {} actions_json 为空，跳过", rule.getId());
            return;
        }

        // dry-run 模式：仅记录将要执行的 action，不真正调用
        if (event.isDryRun()) {
            log.info("[Automation][DRY-RUN] 规则 {} 将执行 {} 个 action: {}",
                    rule.getId(), actionDefs.size(), collectTypes(actionDefs));
            writeLog(rule, event, true, actionDefs.size(), null);
            return;
        }

        // 真正执行：逐 action 执行，异常隔离
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> def : actionDefs) {
            String actionType = String.valueOf(def.get("type"));
            @SuppressWarnings("unchecked")
            Map<String, Object> actionParams = (Map<String, Object>) def.getOrDefault("params", Map.of());
            AutomationAction action = actionMap.get(actionType);
            if (action == null) {
                errors.add("未知 action 类型: " + actionType);
                log.warn("[Automation] 规则 {} 未知 action 类型: {}", rule.getId(), actionType);
                continue;
            }
            try {
                action.execute(actionParams, event);
                successCount++;
            } catch (Exception ex) {
                // 单 action 失败：记录错误，继续执行下一个（异常隔离）
                String errMsg = actionType + ": " + ex.getMessage();
                errors.add(errMsg);
                log.error("[Automation] 规则 {} action {} 执行失败，继续下一个: {}",
                        rule.getId(), actionType, ex.getMessage(), ex);
            }
        }

        boolean allSuccess = errors.isEmpty();
        String errorMsg = errors.isEmpty() ? null : String.join("; ", errors);
        writeLog(rule, event, allSuccess, successCount, errorMsg);
    }

    private void writeLog(SmartTableAutomationRule rule, AutomationEvent event,
                          boolean success, int actionsCount, String errorMsg) {
        try {
            SmartTableAutomationLog logEntry = new SmartTableAutomationLog();
            logEntry.setRuleId(rule.getId());
            logEntry.setTableId(event.getTableId());
            logEntry.setRecordId(event.getRecordId());
            logEntry.setTriggerType(event.getTriggerType().name());
            logEntry.setSuccess(success ? 1 : 0);
            logEntry.setActionsCount(actionsCount);
            logEntry.setErrorMsg(errorMsg != null && errorMsg.length() > 1024
                    ? errorMsg.substring(0, 1024) : errorMsg);
            logEntry.setCreateTime(LocalDateTime.now());
            logService.save(logEntry);
        } catch (Exception e) {
            log.error("[Automation] 写日志失败: ruleId={}", rule.getId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseActions(String actionsJson) {
        if (actionsJson == null || actionsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(actionsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("[Automation] actions_json 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractFieldKey(String triggerConfig) {
        if (triggerConfig == null || triggerConfig.isBlank()) return null;
        try {
            Map<String, Object> cfg = objectMapper.readValue(triggerConfig, Map.class);
            return (String) cfg.get("fieldKey");
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> collectTypes(List<Map<String, Object>> defs) {
        List<String> types = new ArrayList<>();
        for (Map<String, Object> def : defs) {
            types.add(String.valueOf(def.get("type")));
        }
        return types;
    }
}
