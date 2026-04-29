package com.pengcheng.admin.controller.oa;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.oa.shift.dto.ShiftEvaluationResult;
import com.pengcheng.oa.shift.entity.AttendanceShift;
import com.pengcheng.oa.shift.service.AttendanceShiftService;
import com.pengcheng.oa.shift.service.ShiftRuleEngine;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * V4 MVP 闭环② — 班次管理 Controller。
 * URL 与 pengcheng-ui/src/api/oaShift.ts 对齐。
 */
@RestController
@RequestMapping("/admin/oa/shifts")
@RequiredArgsConstructor
public class OaShiftController {

    private final AttendanceShiftService shiftService;
    private final ShiftRuleEngine shiftRuleEngine;

    @GetMapping
    @SaCheckPermission("oa:shift:list")
    public Result<List<AttendanceShift>> list(@RequestParam(required = false) Boolean enabledOnly) {
        return Result.ok(Boolean.TRUE.equals(enabledOnly) ? shiftService.listEnabled() : shiftService.listAll());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("oa:shift:list")
    public Result<AttendanceShift> get(@PathVariable Long id) {
        return Result.ok(shiftService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("oa:shift:add")
    @RepeatSubmit
    @Log(title = "班次", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody AttendanceShift shift) {
        return Result.ok(shiftService.createShift(shift));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("oa:shift:edit")
    @Log(title = "班次", businessType = BusinessType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @RequestBody AttendanceShift shift) {
        shift.setId(id);
        shiftService.updateShift(shift);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("oa:shift:delete")
    @Log(title = "班次", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        shiftService.deleteShift(id);
        return Result.ok();
    }

    /**
     * 班次规则评估：给定班次和实际打卡时间，返回迟到/早退/旷工等判定结果。
     */
    @PostMapping("/{id}/evaluate")
    @SaCheckPermission("oa:shift:list")
    public Result<ShiftEvaluationResult> evaluate(
            @PathVariable Long id,
            @RequestParam LocalDateTime clockIn,
            @RequestParam(required = false) LocalDateTime clockOut) {
        AttendanceShift shift = shiftService.getById(id);
        return Result.ok(shiftRuleEngine.evaluate(shift, clockIn, clockOut));
    }
}
