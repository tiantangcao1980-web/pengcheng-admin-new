package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.unit.dto.LockRequest;
import com.pengcheng.realty.unit.dto.StatusChangeRequest;
import com.pengcheng.realty.unit.dto.UnitMatrix;
import com.pengcheng.realty.unit.entity.RealtyUnit;
import com.pengcheng.realty.unit.service.RealtyUnitService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房源管理控制器
 */
@RestController
@RequestMapping("/admin/realty/units")
@RequiredArgsConstructor
public class UnitController {

    private final RealtyUnitService realtyUnitService;

    /**
     * 房源状态矩阵（楼栋 × 楼层 × 房间），用于前端房源状态图
     */
    @GetMapping("/matrix")
    @SaCheckPermission("realty:unit:list")
    public Result<List<UnitMatrix>> matrix(@RequestParam Long projectId) {
        return Result.ok(realtyUnitService.listMatrix(projectId));
    }

    /**
     * 按楼盘 + 状态筛选房源
     */
    @GetMapping("/by-status")
    @SaCheckPermission("realty:unit:list")
    public Result<List<RealtyUnit>> byStatus(@RequestParam Long projectId,
                                              @RequestParam(required = false) String status) {
        return Result.ok(realtyUnitService.listByStatus(projectId, status));
    }

    /**
     * 获取房源详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("realty:unit:list")
    public Result<RealtyUnit> detail(@PathVariable Long id) {
        return Result.ok(realtyUnitService.getById(id));
    }

    /**
     * 创建房源（自动生成 full_no）
     */
    @PostMapping
    @SaCheckPermission("realty:unit:add")
    @Log(title = "房源管理", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody RealtyUnit unit) {
        return Result.ok(realtyUnitService.create(unit));
    }

    /**
     * 编辑房源基础信息
     */
    @PutMapping("/{id}")
    @SaCheckPermission("realty:unit:edit")
    @Log(title = "房源管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @RequestBody RealtyUnit unit) {
        unit.setId(id);
        realtyUnitService.update(unit);
        return Result.ok();
    }

    /**
     * 删除房源
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("realty:unit:delete")
    @Log(title = "房源管理", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        realtyUnitService.delete(id);
        return Result.ok();
    }

    /**
     * 变更房源状态（状态机校验 + 写审计日志）
     */
    @PostMapping("/{id}/change-status")
    @SaCheckPermission("realty:unit:edit")
    @Log(title = "房源状态变更", businessType = BusinessType.UPDATE)
    public Result<Void> changeStatus(@PathVariable Long id,
                                      @RequestBody StatusChangeRequest req) {
        realtyUnitService.changeStatus(
                id, req.getToStatus(), req.getOperatorId(),
                req.getCustomerId(), req.getDealId(), req.getReason());
        return Result.ok();
    }

    /**
     * 原子锁定房源（AVAILABLE 状态 + 无有效锁才成功）
     */
    @PostMapping("/{id}/lock")
    @SaCheckPermission("realty:unit:edit")
    @Log(title = "房源锁定", businessType = BusinessType.UPDATE)
    public Result<Boolean> lock(@PathVariable Long id, @RequestBody LockRequest req) {
        int hours = req.getHours() != null ? req.getHours() : 2;
        boolean success = realtyUnitService.tryLock(id, req.getUserId(), hours);
        return Result.ok(success);
    }

    /**
     * 解锁房源
     */
    @PostMapping("/{id}/unlock")
    @SaCheckPermission("realty:unit:edit")
    @Log(title = "房源解锁", businessType = BusinessType.UPDATE)
    public Result<Void> unlock(@PathVariable Long id) {
        realtyUnitService.unlock(id);
        return Result.ok();
    }
}
