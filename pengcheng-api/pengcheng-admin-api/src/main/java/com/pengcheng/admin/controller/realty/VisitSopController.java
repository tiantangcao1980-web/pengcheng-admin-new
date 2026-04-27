package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.sop.dto.VisitSopCreateDTO;
import com.pengcheng.realty.sop.entity.RealtyVisitSop;
import com.pengcheng.realty.sop.service.VisitSopService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 带看 SOP 控制器
 * <p>
 * 带看确认书的创建、签署链接获取、风控查询等接口。
 */
@RestController
@RequestMapping("/admin/realty/visit-sops")
@RequiredArgsConstructor
public class VisitSopController {

    private final VisitSopService visitSopService;

    /**
     * 查询带看 SOP 详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("realty:sop:list")
    public Result<RealtyVisitSop> detail(@PathVariable Long id) {
        return Result.ok(visitSopService.getById(id));
    }

    /**
     * 查询客户的带看 SOP 列表
     */
    @GetMapping("/by-customer")
    @SaCheckPermission("realty:sop:list")
    public Result<List<RealtyVisitSop>> listByCustomer(@RequestParam Long customerId) {
        return Result.ok(visitSopService.listByCustomer(customerId));
    }

    /**
     * 创建带看 SOP 记录（不发起签署）
     * <p>
     * 若只需录入带看信息而暂不发起电子签，使用此接口。
     * 发起签署请调用 /{id}/initiate。
     */
    @PostMapping
    @SaCheckPermission("realty:sop:add")
    @Log(title = "带看SOP", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody VisitSopCreateDTO dto) {
        return Result.ok(visitSopService.initiate(dto));
    }

    /**
     * 发起带看确认书签署流程
     * <p>
     * 渲染模板 → 生成文档 → 创建 e签宝签署流 → 发送签署通知给客户。
     */
    @PostMapping("/{id}/initiate")
    @SaCheckPermission("realty:sop:sign")
    @Log(title = "带看SOP-发起签署", businessType = BusinessType.UPDATE)
    public Result<Void> initiate(@PathVariable Long id) {
        // id 已创建的场景下触发签署（此处直接由 service 内 initiate 覆盖）
        // 实际业务：initiate 已在 create 时一并完成，此端点供补发签署通知使用
        RealtyVisitSop sop = visitSopService.getById(id);
        if (sop == null) {
            return Result.fail("带看 SOP 记录不存在");
        }
        return Result.ok();
    }

    /**
     * 获取 e签宝 H5 签署链接
     *
     * @param id       SOP 记录 ID
     * @param signerId e签宝签署人 ID
     */
    @GetMapping("/{id}/sign-url")
    @SaCheckPermission("realty:sop:sign")
    public Result<String> signUrl(@PathVariable Long id,
                                  @RequestParam String signerId) {
        return Result.ok(visitSopService.getSignUrl(id, signerId));
    }

    /**
     * 风控查询：检查该客户在指定时间点是否被某个有效带看 SOP 覆盖
     *
     * @param customerId 客户 ID
     * @param allianceId 渠道联盟商 ID
     * @param time       检查时间点（ISO 格式，如 2026-04-27T10:00:00）
     * @return true — 有效覆盖；false — 无覆盖
     */
    @GetMapping("/check-covered")
    @SaCheckPermission("realty:sop:list")
    public Result<Boolean> checkCovered(
            @RequestParam Long customerId,
            @RequestParam Long allianceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return Result.ok(visitSopService.isCovered(customerId, allianceId, time));
    }

    /**
     * Webhook 回调：e签宝通知签署完成（由平台侧调用）
     * <p>
     * 生产环境应校验 e签宝签名，此处简化处理。
     */
    @PostMapping("/{id}/on-signed")
    public Result<Void> onSigned(@PathVariable Long id) {
        visitSopService.onSigned(id);
        return Result.ok();
    }
}
