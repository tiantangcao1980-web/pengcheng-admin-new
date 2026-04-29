package com.pengcheng.admin.controller.dashboard;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.dashboard.dto.CardRenderResponse;
import com.pengcheng.system.dashboard.dto.RenderRequest;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import com.pengcheng.system.dashboard.entity.DashboardLayout;
import com.pengcheng.system.dashboard.service.DashboardCardService;
import com.pengcheng.system.dashboard.service.DashboardLayoutService;
import com.pengcheng.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;

/**
 * 看板卡片 & 布局管理 Controller（Phase 3 数据决策闭环）。
 *
 * <p>前缀 {@code /admin/dashboard}：
 * <ul>
 *   <li>GET  /cards                         — 按当前用户角色列出可见卡片 metadata</li>
 *   <li>POST /cards/{code}/render           — 渲染指定卡片数据</li>
 *   <li>GET  /layouts/default               — 获取归属者的默认布局</li>
 *   <li>PUT  /layouts                       — 保存（新增/更新）布局</li>
 *   <li>GET  /layouts                       — 列出归属者所有布局</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardCardController {

    private final DashboardCardService cardService;
    private final DashboardLayoutService layoutService;
    private final SysUserService userService;

    // ---------------------------------------------------------------- 卡片 API

    /**
     * 按当前用户角色列出可见的卡片 metadata。
     */
    @GetMapping("/cards")
    public Result<List<DashboardCardDef>> listCards() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<String> roleCodes = userService.getRoleCodes(userId);
        List<DashboardCardDef> cards = cardService.listForRoles(new HashSet<>(roleCodes));
        return Result.ok(cards);
    }

    /**
     * 渲染指定卡片数据。
     *
     * @param code    卡片代码（路径参数）
     * @param request 渲染参数（时间窗口、附加参数；可为空 JSON {}）
     */
    @PostMapping("/cards/{code}/render")
    public Result<CardRenderResponse> renderCard(
            @PathVariable String code,
            @RequestBody(required = false) RenderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        // tenantId 暂用 userId 所属组织 ID；后续接入多租户后替换
        CardRenderResponse resp = cardService.renderCard(code, request, userId, null);
        return Result.ok(resp);
    }

    // ---------------------------------------------------------------- 布局 API

    /**
     * 获取归属者的默认布局。
     *
     * @param ownerType USER / ROLE
     * @param ownerId   归属 ID
     */
    @GetMapping("/layouts/default")
    public Result<DashboardLayout> getDefaultLayout(
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {
        return Result.ok(layoutService.getDefault(ownerType, ownerId));
    }

    /**
     * 保存布局（body.id 为 null 时新增，否则更新）。
     *
     * @param layout 布局实体
     */
    @PutMapping("/layouts")
    public Result<Void> saveLayout(@RequestBody DashboardLayout layout) {
        layoutService.saveLayout(layout);
        return Result.ok();
    }

    /**
     * 列出归属者的所有布局。
     *
     * @param ownerType USER / ROLE
     * @param ownerId   归属 ID
     */
    @GetMapping("/layouts")
    public Result<List<DashboardLayout>> listLayouts(
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {
        return Result.ok(layoutService.listByOwner(ownerType, ownerId));
    }
}
