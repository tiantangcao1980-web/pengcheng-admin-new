package com.pengcheng.admin.controller.oa;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.oa.correction.dto.CorrectionApplyDTO;
import com.pengcheng.oa.correction.entity.AttendanceCorrection;
import com.pengcheng.oa.correction.service.AttendanceCorrectionService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.annotation.RepeatSubmit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * V4 MVP 闭环② — 补卡申请 Controller。
 * URL 与 pengcheng-ui/src/api/oaCorrection.ts 对齐。
 */
@RestController
@RequestMapping("/admin/oa/corrections")
@RequiredArgsConstructor
public class OaCorrectionController {

    private final AttendanceCorrectionService correctionService;

    @GetMapping
    @SaCheckPermission("oa:correction:list")
    public Result<List<AttendanceCorrection>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        Long target = (userId != null) ? userId : StpUtil.getLoginIdAsLong();
        return Result.ok(correctionService.listByUser(target, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("oa:correction:list")
    public Result<AttendanceCorrection> get(@PathVariable Long id) {
        return Result.ok(correctionService.getById(id));
    }

    /**
     * 提交补卡申请：内部会自动启动审批实例（若 dto.flowDefId 提供）。
     */
    @PostMapping
    @SaCheckPermission("oa:correction:add")
    @RepeatSubmit
    @Log(title = "补卡申请", businessType = BusinessType.INSERT)
    public Result<Long> submit(@RequestBody CorrectionApplyDTO dto) {
        if (dto.getUserId() == null) {
            dto.setUserId(StpUtil.getLoginIdAsLong());
        }
        return Result.ok(correctionService.submit(dto));
    }
}
