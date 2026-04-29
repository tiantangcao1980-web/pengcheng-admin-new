package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.workbench.dto.WorkbenchOverviewVO;
import com.pengcheng.system.workbench.service.WorkbenchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 个人工作台聚合接口
 *
 * 一个请求拿到 calendar + todo + project task 三类数据，前端一屏展示。
 */
@RestController
@RequestMapping("/admin/workbench")
@RequiredArgsConstructor
@SaCheckLogin
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    /**
     * 个人工作台概览
     * @param date 视图日期，不传默认今天
     */
    @GetMapping("/overview")
    public Result<WorkbenchOverviewVO> overview(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(workbenchService.getOverview(userId, date));
    }
}
