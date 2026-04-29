package com.pengcheng.admin.controller.openapi;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.openapi.dto.CreateKeyRequest;
import com.pengcheng.system.openapi.dto.CreateKeyResult;
import com.pengcheng.system.openapi.entity.OpenapiKey;
import com.pengcheng.system.openapi.service.OpenapiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OpenAPI 密钥管理（管理员视角）。
 *
 * <p>注意：create / rotate 接口的返回结果含 secretKey 明文，**仅这一次返回**；
 * 之后只能通过 list 看 preview。
 */
@RestController
@RequestMapping("/admin/openapi/keys")
@RequiredArgsConstructor
public class OpenapiKeyController {

    private final OpenapiKeyService keyService;

    @GetMapping
    @SaCheckPermission("openapi:key:list")
    public Result<List<OpenapiKey>> list(@RequestParam Long tenantId) {
        List<OpenapiKey> keys = keyService.listByTenant(tenantId);
        // 安全：移除 hash 字段
        keys.forEach(k -> k.setSecretKeyHash("***"));
        return Result.ok(keys);
    }

    @PostMapping
    @SaCheckPermission("openapi:key:manage")
    public Result<CreateKeyResult> create(@RequestParam Long tenantId, @RequestBody CreateKeyRequest req) {
        return Result.ok(keyService.create(tenantId, StpUtil.getLoginIdAsLong(), req));
    }

    @PostMapping("/{id}/rotate")
    @SaCheckPermission("openapi:key:manage")
    public Result<CreateKeyResult> rotate(@PathVariable Long id) {
        return Result.ok(keyService.rotate(id));
    }

    @PostMapping("/{id}/revoke")
    @SaCheckPermission("openapi:key:manage")
    public Result<Void> revoke(@PathVariable Long id) {
        keyService.revoke(id);
        return Result.ok();
    }
}
