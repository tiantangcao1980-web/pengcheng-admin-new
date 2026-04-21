package com.pengcheng.system.automation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.automation.entity.AutomationLog;
import com.pengcheng.system.automation.entity.AutomationRule;
import com.pengcheng.system.automation.mapper.AutomationLogMapper;
import com.pengcheng.system.automation.mapper.AutomationRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 自动化规则引擎服务
 * <p>
 * 支持基于时间、事件、条件三种触发方式，执行通知、分配、状态更新、任务创建等动作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationService {

    private final AutomationRuleMapper ruleMapper;
    private final AutomationLogMapper logMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取所有启用的规则
     */
    public List<AutomationRule> getEnabledRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .eq(AutomationRule::getEnabled, true)
                .orderByDesc(AutomationRule::getPriority));
    }

    /**
     * 获取所有规则
     */
    public List<AutomationRule> getAllRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .orderByDesc(AutomationRule::getPriority));
    }

    /**
     * 创建规则
     */
    public AutomationRule createRule(AutomationRule rule) {
        rule.setEnabled(true);
        rule.setTriggerCount(0);
        ruleMapper.insert(rule);
        return rule;
    }

    /**
     * 更新规则
     */
    public void updateRule(AutomationRule rule) {
        ruleMapper.updateById(rule);
    }

    /**
     * 启用/禁用规则
     */
    public void toggleRule(Long ruleId, boolean enabled) {
        AutomationRule rule = new AutomationRule();
        rule.setId(ruleId);
        rule.setEnabled(enabled);
        ruleMapper.updateById(rule);
    }

    /**
     * 删除规则
     */
    public void deleteRule(Long ruleId) {
        ruleMapper.deleteById(ruleId);
    }

    /**
     * 执行定时触发类规则（由调度器调用）
     */
    public void executeTimeBasedRules() {
        List<AutomationRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .eq(AutomationRule::getEnabled, true)
                .eq(AutomationRule::getTriggerType, "time_based"));

        for (AutomationRule rule : rules) {
            try {
                executeTimeRule(rule);
            } catch (Exception e) {
                log.error("执行自动化规则失败: rule={}", rule.getName(), e);
                logExecution(rule.getId(), null, "执行失败: " + e.getMessage(), 0);
            }
        }
    }

    /**
     * 执行事件触发类规则
     */
    public void executeEventRules(String eventName, Map<String, Object> eventData) {
        List<AutomationRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .eq(AutomationRule::getEnabled, true)
                .eq(AutomationRule::getTriggerType, "event_based"));

        for (AutomationRule rule : rules) {
            Map<String, Object> config = rule.getTriggerConfig();
            if (config != null && eventName.equals(config.get("event"))) {
                try {
                    executeAction(rule, eventData);
                    logExecution(rule.getId(), eventData, "执行成功", 1);
                    incrementTriggerCount(rule.getId());
                } catch (Exception e) {
                    log.error("执行事件规则失败: rule={}, event={}", rule.getName(), eventName, e);
                    logExecution(rule.getId(), eventData, "执行失败: " + e.getMessage(), 0);
                }
            }
        }
    }

    private void executeTimeRule(AutomationRule rule) {
        Map<String, Object> config = rule.getTriggerConfig();
        if (config == null) return;

        String targetTable = (String) config.get("target_table");
        String checkField = (String) config.get("check_field");
        Object intervalDays = config.get("interval_days");
        Object advanceDays = config.get("advance_days");

        if (targetTable == null || checkField == null) return;

        String sql;
        if (intervalDays != null) {
            sql = String.format(
                    "SELECT id, customer_name FROM %s WHERE deleted = 0 AND %s < DATE_SUB(NOW(), INTERVAL %d DAY)",
                    targetTable, checkField, ((Number) intervalDays).intValue());
        } else if (advanceDays != null) {
            sql = String.format(
                    "SELECT id, customer_name FROM %s WHERE deleted = 0 AND %s BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL %d DAY)",
                    targetTable, checkField, ((Number) advanceDays).intValue());
        } else {
            return;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (!rows.isEmpty()) {
                log.info("自动化规则 [{}] 触发，匹配 {} 条记录", rule.getName(), rows.size());
                for (Map<String, Object> row : rows) {
                    executeAction(rule, row);
                }
                logExecution(rule.getId(), Map.of("matched", rows.size()), "触发成功，匹配" + rows.size() + "条", 1);
                incrementTriggerCount(rule.getId());
            }
        } catch (Exception e) {
            log.warn("查询规则目标数据失败: rule={}, sql={}", rule.getName(), sql, e);
        }
    }

    private void executeAction(AutomationRule rule, Map<String, Object> data) {
        switch (rule.getActionType()) {
            case "notify" -> executeNotifyAction(rule, data);
            case "assign" -> log.info("[Automation] 分配动作待实现: rule={}", rule.getName());
            case "update_status" -> log.info("[Automation] 状态更新待实现: rule={}", rule.getName());
            case "create_task" -> log.info("[Automation] 任务创建待实现: rule={}", rule.getName());
            default -> log.warn("[Automation] 未知动作类型: {}", rule.getActionType());
        }
    }

    private void executeNotifyAction(AutomationRule rule, Map<String, Object> data) {
        Map<String, Object> config = rule.getActionConfig();
        String template = (String) config.getOrDefault("template", "自动化通知");

        String message = template;
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }

        log.info("[Automation] 通知: rule={}, message={}", rule.getName(), message);
    }

    private void logExecution(Long ruleId, Map<String, Object> triggerData, String result, int status) {
        AutomationLog logEntry = new AutomationLog();
        logEntry.setRuleId(ruleId);
        logEntry.setTriggerData(triggerData);
        logEntry.setActionResult(result);
        logEntry.setStatus(status);
        logMapper.insert(logEntry);
    }

    private void incrementTriggerCount(Long ruleId) {
        jdbcTemplate.update("UPDATE sys_automation_rule SET trigger_count = trigger_count + 1, last_triggered_at = NOW() WHERE id = ?", ruleId);
    }

    /**
     * 获取规则执行日志
     */
    public List<AutomationLog> getRuleLogs(Long ruleId, int limit) {
        return logMapper.selectList(new LambdaQueryWrapper<AutomationLog>()
                .eq(AutomationLog::getRuleId, ruleId)
                .orderByDesc(AutomationLog::getExecutedAt)
                .last("LIMIT " + limit));
    }
}
