package com.pengcheng.admin.controller.openapi;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.openapi.entity.OpenapiCallLog;
import com.pengcheng.system.openapi.mapper.OpenapiCallLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 调用日志查询 + 用量统计（M3）。
 */
@Tag(name = "OpenAPI - 调用日志与统计", description = "查询第三方 API 调用记录、聚合用量")
@RestController
@RequestMapping("/admin/openapi")
@RequiredArgsConstructor
public class OpenapiCallLogController {

    private final OpenapiCallLogMapper mapper;

    @Operation(summary = "查询调用日志（按 AK + 时间窗）")
    @GetMapping("/call-logs")
    @SaCheckPermission("openapi:log:read")
    public Result<List<OpenapiCallLog>> list(@RequestParam String accessKey,
                                              @RequestParam(required = false) LocalDateTime startTime,
                                              @RequestParam(required = false) LocalDateTime endTime) {
        LambdaQueryWrapper<OpenapiCallLog> q = new LambdaQueryWrapper<OpenapiCallLog>()
                .eq(OpenapiCallLog::getAccessKey, accessKey)
                .ge(startTime != null, OpenapiCallLog::getCreateTime, startTime)
                .le(endTime != null, OpenapiCallLog::getCreateTime, endTime)
                .orderByDesc(OpenapiCallLog::getId)
                .last("LIMIT 200");
        return Result.ok(mapper.selectList(q));
    }

    @Operation(summary = "聚合用量统计（按 AK 当月调用数 + 错误数）")
    @GetMapping("/usage")
    @SaCheckPermission("openapi:log:read")
    public Result<UsageSummary> usage(@RequestParam String accessKey) {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Long total = mapper.selectCount(new LambdaQueryWrapper<OpenapiCallLog>()
                .eq(OpenapiCallLog::getAccessKey, accessKey)
                .ge(OpenapiCallLog::getCreateTime, monthStart));
        Long errors = mapper.selectCount(new LambdaQueryWrapper<OpenapiCallLog>()
                .eq(OpenapiCallLog::getAccessKey, accessKey)
                .ge(OpenapiCallLog::getCreateTime, monthStart)
                .ge(OpenapiCallLog::getStatusCode, 400));

        UsageSummary u = new UsageSummary();
        u.setTotalThisMonth(total != null ? total : 0);
        u.setErrorThisMonth(errors != null ? errors : 0);
        u.setSuccessRate(total != null && total > 0
                ? (1.0 - (double) (errors != null ? errors : 0) / total) : 1.0);
        return Result.ok(u);
    }

    @Data
    public static class UsageSummary {
        private Long totalThisMonth;
        private Long errorThisMonth;
        private Double successRate;
    }
}
