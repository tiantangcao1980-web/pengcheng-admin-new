package com.pengcheng.crm.customfield.controller;

import com.pengcheng.common.result.Result;
import com.pengcheng.crm.customfield.entity.CustomFieldDef;
import com.pengcheng.crm.customfield.service.CustomFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 自定义字段定义/取值 API
 */
@RestController
@RequestMapping("/api/crm/custom-fields")
public class CustomFieldController {

    @Autowired
    private CustomFieldService customFieldService;

    @PostMapping("/defs")
    public Result<CustomFieldDef> createDef(@RequestBody CustomFieldDef def) {
        return Result.ok(customFieldService.createDef(def));
    }

    @GetMapping("/defs")
    public Result<List<CustomFieldDef>> listDefs(@RequestParam String entityType) {
        return Result.ok(customFieldService.listDefs(entityType));
    }

    @PutMapping("/values")
    public Result<Void> saveValues(@RequestParam String entityType,
                                   @RequestParam Long entityId,
                                   @RequestBody Map<String, Object> values) {
        customFieldService.saveValues(entityType, entityId, values);
        return Result.ok();
    }

    @GetMapping("/values")
    public Result<Map<String, Object>> loadValues(@RequestParam String entityType,
                                                   @RequestParam Long entityId) {
        return Result.ok(customFieldService.loadValues(entityType, entityId));
    }
}
