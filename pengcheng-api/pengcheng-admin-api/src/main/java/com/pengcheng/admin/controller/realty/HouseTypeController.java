package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.unit.entity.HouseType;
import com.pengcheng.realty.unit.service.HouseTypeService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 户型管理控制器
 */
@RestController
@RequestMapping("/admin/realty/house-types")
@RequiredArgsConstructor
public class HouseTypeController {

    private final HouseTypeService houseTypeService;

    /**
     * 按楼盘查询户型列表（全部）
     */
    @GetMapping("/by-project")
    @SaCheckPermission("realty:unit:list")
    public Result<List<HouseType>> byProject(@RequestParam Long projectId) {
        return Result.ok(houseTypeService.listByProject(projectId));
    }

    /**
     * 按楼盘查询启用户型
     */
    @GetMapping("/enabled")
    @SaCheckPermission("realty:unit:list")
    public Result<List<HouseType>> enabled(@RequestParam Long projectId) {
        return Result.ok(houseTypeService.listEnabled(projectId));
    }

    /**
     * 获取户型详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("realty:unit:list")
    public Result<HouseType> detail(@PathVariable Long id) {
        return Result.ok(houseTypeService.getById(id));
    }

    /**
     * 创建户型
     */
    @PostMapping
    @SaCheckPermission("realty:unit:add")
    @Log(title = "户型管理", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody HouseType houseType) {
        return Result.ok(houseTypeService.create(houseType));
    }

    /**
     * 编辑户型
     */
    @PutMapping("/{id}")
    @SaCheckPermission("realty:unit:edit")
    @Log(title = "户型管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @RequestBody HouseType houseType) {
        houseType.setId(id);
        houseTypeService.update(houseType);
        return Result.ok();
    }

    /**
     * 删除户型
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("realty:unit:delete")
    @Log(title = "户型管理", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        houseTypeService.delete(id);
        return Result.ok();
    }
}
