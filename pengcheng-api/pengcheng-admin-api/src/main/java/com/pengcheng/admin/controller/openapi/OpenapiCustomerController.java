package com.pengcheng.admin.controller.openapi;

import com.pengcheng.common.result.Result;
import com.pengcheng.system.openapi.interceptor.OpenapiContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 第三方开放 API 示例 — 客户域只读接口（V1.2 开放 API 平台示例端点，M2 演示）。
 *
 * <p>调用方式：
 * <ul>
 *   <li>4 头 X-Openapi-{Access-Key, Timestamp, Nonce, Signature} 经 OpenapiAuthInterceptor 校验通过</li>
 *   <li>scope 必须含 {@code customer:read} 或 {@code customer:*} 或 {@code *}</li>
 *   <li>每分钟限流配额由 OpenapiKey.rateLimit 控制</li>
 * </ul>
 */
@Tag(name = "OpenAPI - 客户域", description = "第三方开发者读取客户数据的开放接口")
@RestController
@RequestMapping("/openapi/v1/customers")
@SecurityRequirement(name = "OpenAPI-Signature")
public class OpenapiCustomerController {

    @Operation(summary = "查询客户列表（最多 50 条）",
               description = "需要 scope: customer:read")
    @GetMapping
    public Result<List<CustomerVO>> list(
            @Parameter(description = "页码，从 1 开始") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小（≤50）") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        // 示例：实际应注入 CustomerService 按 OpenapiContext.currentTenantId() 过滤
        Long tenantId = OpenapiContext.currentTenantId();
        // TODO 真实查询：customerService.listForOpenapi(tenantId, page, Math.min(size, 50))
        return Result.ok(List.of());
    }

    @Operation(summary = "查询单个客户",
               description = "需要 scope: customer:read")
    @GetMapping("/{id}")
    public Result<CustomerVO> get(@Parameter(description = "客户 ID") @PathVariable Long id) {
        Long tenantId = OpenapiContext.currentTenantId();
        // TODO 真实查询 + 校验客户属于该 tenant
        return Result.ok(null);
    }

    /** 暴露给第三方的客户视图（脱敏：手机号末 4 位掩码、不返回身份证号等敏感字段）。 */
    @Data
    public static class CustomerVO {
        @io.swagger.v3.oas.annotations.media.Schema(description = "客户 ID")
        private Long id;

        @io.swagger.v3.oas.annotations.media.Schema(description = "客户姓名")
        private String name;

        @io.swagger.v3.oas.annotations.media.Schema(description = "手机号（末 4 位掩码）", example = "138****1234")
        private String phoneMasked;

        @io.swagger.v3.oas.annotations.media.Schema(description = "客户来源")
        private String source;

        @io.swagger.v3.oas.annotations.media.Schema(description = "创建时间 ISO-8601")
        private String createdAt;
    }
}
