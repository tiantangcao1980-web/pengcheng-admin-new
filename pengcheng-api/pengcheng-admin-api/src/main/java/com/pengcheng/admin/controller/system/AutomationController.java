package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.automation.entity.AutomationLog;
import com.pengcheng.system.automation.entity.AutomationRule;
import com.pengcheng.system.automation.service.AutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自动化规则管理接口
 */
@RestController
@RequestMapping("/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @GetMapping("/rules")
    public Result<List<AutomationRule>> listRules() {
        return Result.ok(automationService.getAllRules());
    }

    @PostMapping("/rule")
    public Result<AutomationRule> createRule(@RequestBody AutomationRule rule) {
        rule.setCreatedBy(StpUtil.getLoginIdAsLong());
        return Result.ok(automationService.createRule(rule));
    }

    @PutMapping("/rule")
    public Result<Void> updateRule(@RequestBody AutomationRule rule) {
        automationService.updateRule(rule);
        return Result.ok();
    }

    @PostMapping("/rule/{id}/toggle")
    public Result<Void> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        automationService.toggleRule(id, enabled);
        return Result.ok();
    }

    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        automationService.deleteRule(id);
        return Result.ok();
    }

    @GetMapping("/rule/{id}/logs")
    public Result<List<AutomationLog>> getRuleLogs(@PathVariable Long id, @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(automationService.getRuleLogs(id, limit));
    }

    /**
     * 手动触发规则执行（调试用）
     */
    @PostMapping("/execute")
    public Result<Void> manualExecute() {
        automationService.executeTimeBasedRules();
        return Result.ok();
    }
}
