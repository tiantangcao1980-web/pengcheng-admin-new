package com.pengcheng.auth.tenant;

import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.service.SysRoleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 角色数据权限作用域配置控制器（仅本人 / 本部门 / 本部门及下级 / 全部）。
 *
 * <p>对应 sys_role.data_scope：1=全部, 2=自定义, 3=本部门, 4=本部门及以下, 5=仅本人。
 *
 * <p>路由 /auth/role/data-scope/*。
 */
@RestController
@RequestMapping("/auth/role/data-scope")
@RequiredArgsConstructor
public class RoleDataScopeController {

    private static final List<Integer> VALID_SCOPES = Arrays.asList(1, 2, 3, 4, 5);

    private final SysRoleService roleService;

    /**
     * 列出所有作用域选项（供前端下拉）
     */
    @GetMapping("/options")
    public Result<List<Map<String, Object>>> options() {
        return Result.ok(List.of(
                Map.of("value", 1, "label", "全部"),
                Map.of("value", 2, "label", "自定义部门"),
                Map.of("value", 3, "label", "本部门"),
                Map.of("value", 4, "label", "本部门及下级"),
                Map.of("value", 5, "label", "仅本人")
        ));
    }

    /**
     * 修改某角色的数据范围
     */
    @PutMapping("/{roleId}")
    public Result<Void> update(@PathVariable Long roleId, @RequestBody UpdateScopeRequest req) {
        if (req.getDataScope() == null || !VALID_SCOPES.contains(req.getDataScope())) {
            throw new BusinessException("无效的数据范围值");
        }
        SysRole role = roleService.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setDataScope(req.getDataScope());
        roleService.updateById(role);
        return Result.ok();
    }

    @Data
    public static class UpdateScopeRequest {
        private Integer dataScope;
    }
}
