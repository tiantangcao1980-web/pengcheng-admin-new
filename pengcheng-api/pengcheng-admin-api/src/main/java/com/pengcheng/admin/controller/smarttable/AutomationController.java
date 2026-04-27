package com.pengcheng.admin.controller.smarttable;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.smarttable.automation.AutomationDispatcher;
import com.pengcheng.system.smarttable.automation.AutomationEvent;
import com.pengcheng.system.smarttable.automation.AutomationTriggerType;
import com.pengcheng.system.smarttable.automation.dto.AutomationRuleRequest;
import com.pengcheng.system.smarttable.automation.dto.AutomationTestRequest;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationLog;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationRule;
import com.pengcheng.system.smarttable.automation.service.AutomationLogService;
import com.pengcheng.system.smarttable.automation.service.AutomationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 多维表格自动化规则接口
 */
@RestController
@RequestMapping("/admin/smarttable/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationRuleService ruleService;
    private final AutomationLogService logService;
    private final AutomationDispatcher dispatcher;

    // ==================== 规则 CRUD ====================

    /**
     * 分页查询规则列表
     */
    @GetMapping("/rules")
    @SaCheckPermission("smarttable:automation:list")
    public Result<Page<SmartTableAutomationRule>> listRules(
            @RequestParam Long tableId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(ruleService.pageByTable(tableId, page, pageSize));
    }

    /**
     * 创建规则
     */
    @PostMapping("/rules")
    @SaCheckPermission("smarttable:automation:create")
    @Log(title = "多维表格自动化", businessType = BusinessType.INSERT)
    public Result<SmartTableAutomationRule> createRule(@RequestBody AutomationRuleRequest req) {
        SmartTableAutomationRule rule = toEntity(req, null);
        rule.setCreateBy(Long.parseLong(String.valueOf(StpUtil.getLoginId())));
        return Result.ok(ruleService.create(rule));
    }

    /**
     * 更新规则
     */
    @PutMapping("/rules/{id}")
    @SaCheckPermission("smarttable:automation:update")
    @Log(title = "多维表格自动化", businessType = BusinessType.UPDATE)
    public Result<SmartTableAutomationRule> updateRule(@PathVariable Long id,
                                                       @RequestBody AutomationRuleRequest req) {
        SmartTableAutomationRule rule = toEntity(req, id);
        return Result.ok(ruleService.update(rule));
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/rules/{id}")
    @SaCheckPermission("smarttable:automation:delete")
    @Log(title = "多维表格自动化", businessType = BusinessType.DELETE)
    public Result<Void> deleteRule(@PathVariable Long id) {
        ruleService.delete(id);
        return Result.ok();
    }

    // ==================== 试运行 ====================

    /**
     * 试运行规则（dry-run）：评估条件 + 解析 action，不真正执行副作用
     */
    @PostMapping("/rules/{id}/test")
    @SaCheckPermission("smarttable:automation:test")
    @Log(title = "多维表格自动化试运行", businessType = BusinessType.OTHER)
    public Result<String> testRule(@PathVariable Long id,
                                   @RequestBody(required = false) AutomationTestRequest req) {
        SmartTableAutomationRule rule = ruleService.getById(id);
        if (rule == null) {
            return Result.fail("规则不存在");
        }
        if (req == null) req = new AutomationTestRequest();

        String triggerTypeName = req.getTriggerType() != null
                ? req.getTriggerType() : rule.getTriggerType();

        AutomationEvent event = AutomationEvent.builder()
                .tableId(rule.getTableId())
                .recordId(req.getRecordId())
                .triggerType(AutomationTriggerType.of(triggerTypeName))
                .newRow(req.getNewRow())
                .oldRow(req.getOldRow())
                .fieldKey(req.getFieldKey())
                .dryRun(true)
                .build();

        // 临时将规则 enabled 设为 1 以保证匹配（试运行忽略启用状态通过直接 dispatch 单条规则）
        int originEnabled = rule.getEnabled() != null ? rule.getEnabled() : 1;
        rule.setEnabled(1);
        dispatcher.dispatch(event);
        rule.setEnabled(originEnabled);

        return Result.ok("dry-run 完成，请查看日志");
    }

    // ==================== 日志查询 ====================

    /**
     * 分页查询执行日志
     */
    @GetMapping("/logs")
    @SaCheckPermission("smarttable:automation:logs")
    public Result<Page<SmartTableAutomationLog>> listLogs(
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) Long tableId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(logService.page(ruleId, tableId, page, pageSize));
    }

    // ==================== 内部方法 ====================

    private SmartTableAutomationRule toEntity(AutomationRuleRequest req, Long id) {
        SmartTableAutomationRule rule = new SmartTableAutomationRule();
        rule.setId(id);
        rule.setTableId(req.getTableId());
        rule.setName(req.getName());
        rule.setEnabled(req.getEnabled() != null ? req.getEnabled() : 1);
        rule.setTriggerType(req.getTriggerType());
        rule.setTriggerConfig(req.getTriggerConfig());
        rule.setConditionJson(req.getConditionJson());
        rule.setActionsJson(req.getActionsJson());
        return rule;
    }
}
