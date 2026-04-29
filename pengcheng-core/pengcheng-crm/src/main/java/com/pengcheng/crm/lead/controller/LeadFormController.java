package com.pengcheng.crm.lead.controller;

import com.pengcheng.common.result.Result;
import com.pengcheng.crm.lead.dto.PublicLeadSubmitDTO;
import com.pengcheng.crm.lead.entity.CrmLead;
import com.pengcheng.crm.lead.entity.CrmLeadForm;
import com.pengcheng.crm.lead.service.LeadFormService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/lead-forms")
public class LeadFormController {

    @Autowired
    private LeadFormService formService;

    /** 管理员：创建采集表单 */
    @PostMapping
    public Result<CrmLeadForm> create(@RequestBody CrmLeadForm form) {
        return Result.ok(formService.createForm(form));
    }

    /** 公开：根据 formCode 获取 schema（仅 enabled 表单） */
    @GetMapping("/public/{code}")
    public Result<CrmLeadForm> publicGet(@PathVariable String code) {
        CrmLeadForm form = formService.getByCode(code);
        // 出于安全：管理字段不下发
        form.setDefaultOwnerId(null);
        return Result.ok(form);
    }

    /** 公开：表单提交 -> 创建线索 */
    @PostMapping("/public/submit")
    public Result<CrmLead> submit(@RequestBody @Valid PublicLeadSubmitDTO dto) {
        return Result.ok(formService.submit(dto));
    }
}
