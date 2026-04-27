package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.sop.dto.CommissionInitiateDTO;
import com.pengcheng.realty.sop.entity.RealtyCommissionTripartite;
import com.pengcheng.realty.sop.service.CommissionTripartiteService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 佣金三方协议控制器
 * <p>
 * 佣金三方单的创建、签署链接获取等接口。
 * 通常由成交回调自动触发，也支持管理员手动发起。
 */
@RestController
@RequestMapping("/admin/realty/commission-tripartites")
@RequiredArgsConstructor
public class CommissionTripartiteController {

    private final CommissionTripartiteService commissionTripartiteService;

    /**
     * 查询三方协议详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("realty:sop:list")
    public Result<RealtyCommissionTripartite> detail(@PathVariable Long id) {
        return Result.ok(commissionTripartiteService.getById(id));
    }

    /**
     * 按成交单 ID 查询三方协议
     */
    @GetMapping("/by-deal/{dealId}")
    @SaCheckPermission("realty:sop:list")
    public Result<RealtyCommissionTripartite> byDealId(@PathVariable Long dealId) {
        return Result.ok(commissionTripartiteService.getByDealId(dealId));
    }

    /**
     * 查询渠道的三方协议列表
     */
    @GetMapping("/by-alliance")
    @SaCheckPermission("realty:sop:list")
    public Result<List<RealtyCommissionTripartite>> listByAlliance(@RequestParam Long allianceId) {
        return Result.ok(commissionTripartiteService.listByAlliance(allianceId));
    }

    /**
     * 手动发起佣金三方协议签署流程
     */
    @PostMapping
    @SaCheckPermission("realty:sop:sign")
    @Log(title = "佣金三方协议-发起", businessType = BusinessType.INSERT)
    public Result<Long> initiate(@RequestBody CommissionInitiateDTO dto) {
        return Result.ok(commissionTripartiteService.initiate(dto));
    }

    /**
     * 按成交单 ID 发起佣金三方协议（快捷接口）
     * <p>
     * 由成交回调触发时使用，dealId 需已在请求体中提供对应数据。
     */
    @PostMapping("/{dealId}/initiate")
    @SaCheckPermission("realty:sop:sign")
    @Log(title = "佣金三方协议-按成交单发起", businessType = BusinessType.INSERT)
    public Result<Long> initiateByDeal(@PathVariable Long dealId,
                                       @RequestBody CommissionInitiateDTO dto) {
        dto.setDealId(dealId);
        return Result.ok(commissionTripartiteService.initiate(dto));
    }

    /**
     * 获取 e签宝 H5 签署链接
     *
     * @param id       三方协议记录 ID
     * @param signerId e签宝签署人 ID
     */
    @GetMapping("/{id}/sign-url")
    @SaCheckPermission("realty:sop:sign")
    public Result<String> signUrl(@PathVariable Long id,
                                  @RequestParam String signerId) {
        return Result.ok(commissionTripartiteService.getSignUrl(id, signerId));
    }

    /**
     * Webhook 回调：e签宝通知全部签署完成（由平台侧调用）
     */
    @PostMapping("/{id}/on-signed")
    public Result<Void> onSigned(@PathVariable Long id) {
        commissionTripartiteService.onSigned(id);
        return Result.ok();
    }
}
