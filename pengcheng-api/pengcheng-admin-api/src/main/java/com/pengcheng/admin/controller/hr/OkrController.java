package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.okr.ai.OkrSuggestionService;
import com.pengcheng.hr.okr.dto.CheckinDTO;
import com.pengcheng.hr.okr.dto.CreateObjectiveDTO;
import com.pengcheng.hr.okr.dto.UpdateProgressDTO;
import com.pengcheng.hr.okr.entity.OkrCheckin;
import com.pengcheng.hr.okr.entity.OkrKeyResult;
import com.pengcheng.hr.okr.entity.OkrObjective;
import com.pengcheng.hr.okr.entity.OkrPeriod;
import com.pengcheng.hr.okr.service.OkrCheckinService;
import com.pengcheng.hr.okr.service.OkrKeyResultService;
import com.pengcheng.hr.okr.service.OkrObjectiveService;
import com.pengcheng.hr.okr.service.OkrPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OKR 目标管理 Controller
 * <p>
 * 权限前缀：hr:okr:*
 * </p>
 */
@Tag(name = "OKR 目标管理", description = "OKR 周期、目标、关键结果、Check-in、AI 建议")
@RestController
@RequestMapping("/admin/hr/okr")
@RequiredArgsConstructor
public class OkrController {

    private final OkrPeriodService periodService;
    private final OkrObjectiveService objectiveService;
    private final OkrKeyResultService keyResultService;
    private final OkrCheckinService checkinService;
    private final OkrSuggestionService suggestionService;

    // ==================== OKR 周期 ====================

    @Operation(summary = "获取所有周期（含草稿/进行中/已结束）")
    @GetMapping("/periods")
    public Result<List<OkrPeriod>> listPeriods() {
        return Result.ok(periodService.listAll());
    }

    @Operation(summary = "获取进行中的周期")
    @GetMapping("/periods/active")
    public Result<List<OkrPeriod>> listActivePeriods() {
        return Result.ok(periodService.listActive());
    }

    @Operation(summary = "创建 OKR 周期")
    @PostMapping("/periods")
    @SaCheckPermission("hr:okr:period:write")
    public Result<Long> createPeriod(@RequestBody OkrPeriod period) {
        return Result.ok(periodService.create(period));
    }

    @Operation(summary = "激活 OKR 周期（草稿 → 进行中）")
    @PutMapping("/periods/{id}/activate")
    @SaCheckPermission("hr:okr:period:write")
    public Result<Void> activatePeriod(@PathVariable Long id) {
        periodService.activatePeriod(id);
        return Result.ok();
    }

    @Operation(summary = "关闭 OKR 周期（进行中 → 已结束）")
    @PutMapping("/periods/{id}/close")
    @SaCheckPermission("hr:okr:period:write")
    public Result<Void> closePeriod(@PathVariable Long id) {
        periodService.closePeriod(id);
        return Result.ok();
    }

    // ==================== OKR 目标 ====================

    @Operation(summary = "按 owner + period 查询目标列表")
    @GetMapping("/objectives")
    public Result<List<OkrObjective>> listObjectives(
            @RequestParam Long ownerId,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) Long periodId) {
        return Result.ok(objectiveService.listByOwnerAndPeriod(ownerId, ownerType, periodId));
    }

    @Operation(summary = "查询目标对齐树（子节点列表）")
    @GetMapping("/objectives/tree")
    public Result<List<OkrObjective>> listAlignTree(
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) Long parentId) {
        return Result.ok(objectiveService.listAlignTree(periodId, parentId));
    }

    @Operation(summary = "创建目标")
    @PostMapping("/objectives")
    @SaCheckPermission("hr:okr:objective:write")
    public Result<Long> createObjective(@RequestBody CreateObjectiveDTO dto) {
        return Result.ok(objectiveService.create(dto));
    }

    @Operation(summary = "更新目标")
    @PutMapping("/objectives")
    @SaCheckPermission("hr:okr:objective:write")
    public Result<Void> updateObjective(@RequestBody OkrObjective objective) {
        objectiveService.update(objective);
        return Result.ok();
    }

    @Operation(summary = "删除目标")
    @DeleteMapping("/objectives/{id}")
    @SaCheckPermission("hr:okr:objective:write")
    public Result<Void> deleteObjective(@PathVariable Long id) {
        objectiveService.delete(id);
        return Result.ok();
    }

    // ==================== 关键结果 ====================

    @Operation(summary = "查询目标下所有 KR")
    @GetMapping("/key-results")
    public Result<List<OkrKeyResult>> listKeyResults(@RequestParam Long objectiveId) {
        return Result.ok(keyResultService.listByObjective(objectiveId));
    }

    @Operation(summary = "新建 KR")
    @PostMapping("/key-results")
    @SaCheckPermission("hr:okr:kr:write")
    public Result<Long> createKeyResult(@RequestBody OkrKeyResult keyResult) {
        return Result.ok(keyResultService.create(keyResult));
    }

    @Operation(summary = "更新 KR 基本信息")
    @PutMapping("/key-results")
    @SaCheckPermission("hr:okr:kr:write")
    public Result<Void> updateKeyResult(@RequestBody OkrKeyResult keyResult) {
        keyResultService.update(keyResult);
        return Result.ok();
    }

    @Operation(summary = "更新 KR 当前值（自动联动 Objective 进度）")
    @PutMapping("/key-results/progress")
    @SaCheckPermission("hr:okr:kr:write")
    public Result<Void> updateKrProgress(@RequestBody UpdateProgressDTO dto) {
        keyResultService.updateCurrentValue(dto);
        return Result.ok();
    }

    // ==================== Check-in ====================

    @Operation(summary = "提交 Check-in")
    @PostMapping("/checkins")
    @SaCheckPermission("hr:okr:checkin:write")
    public Result<Long> submitCheckin(@RequestBody CheckinDTO dto) {
        return Result.ok(checkinService.submit(dto));
    }

    @Operation(summary = "查询目标 Check-in 列表")
    @GetMapping("/checkins")
    public Result<List<OkrCheckin>> listCheckins(@RequestParam Long objectiveId) {
        return Result.ok(checkinService.listByObjective(objectiveId));
    }

    @Operation(summary = "查询用户某周期所有 Check-in")
    @GetMapping("/checkins/user")
    public Result<List<OkrCheckin>> listCheckinsByUserPeriod(
            @RequestParam Long userId,
            @RequestParam Long periodId) {
        return Result.ok(checkinService.listByUserPeriod(userId, periodId));
    }

    // ==================== AI 辅助 ====================

    @Operation(summary = "AI 辅助拆解：根据目标生成 KR 建议")
    @PostMapping("/objectives/{id}/suggest-key-results")
    public Result<List<String>> suggestKeyResults(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        // 优先用传入参数，否则从数据库查
        String objTitle = title;
        String objDesc = description;
        if (objTitle == null) {
            OkrObjective obj = objectiveService.listAlignTree(null, null)
                    .stream()
                    .filter(o -> id.equals(o.getId()))
                    .findFirst()
                    .orElse(null);
            if (obj != null) {
                objTitle = obj.getTitle();
                objDesc = obj.getDescription();
            }
        }
        return Result.ok(suggestionService.suggestKeyResults(objTitle, objDesc));
    }
}
