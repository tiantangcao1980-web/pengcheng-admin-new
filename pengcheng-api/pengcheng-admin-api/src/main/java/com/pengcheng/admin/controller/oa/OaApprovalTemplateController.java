package com.pengcheng.admin.controller.oa;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.oa.template.entity.ApprovalTemplate;
import com.pengcheng.oa.template.service.ApprovalTemplateService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.annotation.RepeatSubmit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * V4 MVP 闭环② — 审批模板 Controller（外出/加班/报销/通用）。
 * URL 与 pengcheng-ui/src/api/oaApprovalTemplate.ts 对齐。
 */
@RestController
@RequestMapping("/admin/oa/approval-templates")
@RequiredArgsConstructor
public class OaApprovalTemplateController {

    private final ApprovalTemplateService templateService;

    @GetMapping
    @SaCheckPermission("oa:approval-template:list")
    public Result<List<ApprovalTemplate>> list(@RequestParam(required = false) Boolean enabledOnly) {
        return Result.ok(Boolean.TRUE.equals(enabledOnly) ? templateService.listEnabled() : templateService.listAll());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("oa:approval-template:list")
    public Result<ApprovalTemplate> get(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    @GetMapping("/by-code/{code}")
    @SaCheckPermission("oa:approval-template:list")
    public Result<ApprovalTemplate> getByCode(@PathVariable String code) {
        return Result.ok(templateService.getByCode(code));
    }

    @PostMapping
    @SaCheckPermission("oa:approval-template:add")
    @RepeatSubmit
    @Log(title = "审批模板", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody ApprovalTemplate template) {
        return Result.ok(templateService.createTemplate(template));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("oa:approval-template:edit")
    @Log(title = "审批模板", businessType = BusinessType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @RequestBody ApprovalTemplate template) {
        template.setId(id);
        templateService.updateTemplate(template);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("oa:approval-template:delete")
    @Log(title = "审批模板", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.ok();
    }
}
