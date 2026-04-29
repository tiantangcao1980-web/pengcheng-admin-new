package com.pengcheng.crm.lead.controller;

import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.crm.lead.dto.LeadAssignDTO;
import com.pengcheng.crm.lead.dto.LeadConvertDTO;
import com.pengcheng.crm.lead.dto.LeadCreateDTO;
import com.pengcheng.crm.lead.entity.CrmLead;
import com.pengcheng.crm.lead.entity.CrmLeadAssignment;
import com.pengcheng.crm.lead.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/crm/leads")
public class LeadController {

    @Autowired
    private LeadService leadService;

    @PostMapping
    public Result<CrmLead> create(@RequestBody @Valid LeadCreateDTO dto) {
        return Result.ok(leadService.create(dto));
    }

    @GetMapping
    public Result<PageResult<CrmLead>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(leadService.page(page, size, ownerId, status, keyword)));
    }

    @GetMapping("/{id}")
    public Result<CrmLead> get(@PathVariable Long id) {
        return Result.ok(leadService.getById(id));
    }

    @PostMapping("/assign")
    public Result<Integer> assign(@RequestBody @Valid LeadAssignDTO dto) {
        // currentUserId 在 V4 实际部署里由鉴权框架注入；MVP 占位 0L 由上层网关补
        return Result.ok(leadService.assign(dto, 0L, new HashMap<>()));
    }

    @PostMapping("/convert")
    public Result<CrmLead> convert(@RequestBody @Valid LeadConvertDTO dto) {
        return Result.ok(leadService.convertToCustomer(dto));
    }

    @GetMapping("/{id}/assignments")
    public Result<List<CrmLeadAssignment>> assignments(@PathVariable Long id) {
        return Result.ok(leadService.assignmentLog(id));
    }
}
