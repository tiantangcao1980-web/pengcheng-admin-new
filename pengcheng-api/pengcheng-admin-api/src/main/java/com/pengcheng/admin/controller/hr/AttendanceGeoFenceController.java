package com.pengcheng.admin.controller.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.result.Result;
import com.pengcheng.hr.attendance.entity.GeoFence;
import com.pengcheng.hr.attendance.mapper.GeoFenceMapper;
import com.pengcheng.hr.attendance.service.GeoFenceCacheService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考勤地理围栏管理（多围栏，公司级）。
 *
 * <p>所有写操作完成后会调用 {@link GeoFenceCacheService#refresh()} 刷新内存缓存。
 */
@RestController
@RequestMapping("/admin/attendance/fence")
@RequiredArgsConstructor
public class AttendanceGeoFenceController {

    private final GeoFenceMapper geoFenceMapper;
    private final GeoFenceCacheService geoFenceCacheService;

    /** 列出全部围栏（含禁用） */
    @GetMapping("/list")
    @SaCheckPermission("hr:attendance:fence:manage")
    public Result<List<GeoFence>> list() {
        LambdaQueryWrapper<GeoFence> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GeoFence::getId);
        return Result.ok(geoFenceMapper.selectList(wrapper));
    }

    /** 新增围栏 */
    @PostMapping
    @SaCheckPermission("hr:attendance:fence:manage")
    @Log(title = "考勤围栏-新增", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody GeoFence fence) {
        if (fence.getActive() == null) fence.setActive(1);
        geoFenceMapper.insert(fence);
        geoFenceCacheService.refresh();
        return Result.ok(fence.getId());
    }

    /** 更新围栏 */
    @PutMapping("/{id}")
    @SaCheckPermission("hr:attendance:fence:manage")
    @Log(title = "考勤围栏-更新", businessType = BusinessType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @RequestBody GeoFence fence) {
        fence.setId(id);
        geoFenceMapper.updateById(fence);
        geoFenceCacheService.refresh();
        return Result.ok();
    }

    /** 删除围栏（逻辑删除，由 BaseEntity.deleted 控制） */
    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:attendance:fence:manage")
    @Log(title = "考勤围栏-删除", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        geoFenceMapper.deleteById(id);
        geoFenceCacheService.refresh();
        return Result.ok();
    }
}
