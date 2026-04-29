package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.commission.dto.CommissionApprovalActionDTO;
import com.pengcheng.realty.commission.dto.CommissionAuditDTO;
import com.pengcheng.realty.commission.dto.CommissionCreateDTO;
import com.pengcheng.realty.commission.dto.CommissionQueryDTO;
import com.pengcheng.realty.commission.dto.CommissionSubmitDTO;
import com.pengcheng.realty.commission.dto.CommissionVO;
import com.pengcheng.realty.commission.entity.CommissionApproval;
import com.pengcheng.realty.commission.entity.CommissionChangeLog;
import com.pengcheng.realty.commission.mapper.CommissionApprovalMapper;
import com.pengcheng.realty.commission.service.CommissionService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成交佣金管理控制器
 */
@RestController
@RequestMapping("/admin/commission")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;
    private final CommissionApprovalMapper commissionApprovalMapper;

    /**
     * 佣金分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("realty:commission:list")
    public Result<PageResult<CommissionVO>> page(CommissionQueryDTO query) {
        return Result.ok(commissionService.pageCommissions(query));
    }

    /**
     * 录入佣金
     */
    @PostMapping("/create")
    @SaCheckPermission("realty:commission:add")
    @Log(title = "佣金管理", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody CommissionCreateDTO dto) {
        return Result.ok(commissionService.createCommission(dto));
    }

    /**
     * 财务审核佣金（旧单级审批接口，保留兼容）
     * 新调用方请使用 /submit + /approve/manager + /approve/finance + /approve/payment 多级流程
     */
    @Deprecated
    @PostMapping("/audit")
    @SaCheckPermission("realty:commission:audit")
    @Log(title = "佣金审核", businessType = BusinessType.UPDATE)
    public Result<Void> audit(@RequestBody CommissionAuditDTO dto) {
        commissionService.auditCommission(dto);
        return Result.ok();
    }

    // ============================================================
    // 多级审批流（业务员 → 主管 → 财务 → 放款）
    // ============================================================

    /**
     * 业务员提交佣金审批
     */
    @PostMapping("/submit")
    @SaCheckPermission("realty:commission:submit")
    @Log(title = "佣金提交审批", businessType = BusinessType.UPDATE)
    public Result<Void> submit(@RequestBody CommissionSubmitDTO dto) {
        commissionService.submitForApproval(dto);
        return Result.ok();
    }

    /**
     * 主管审批
     */
    @PostMapping("/approve/manager")
    @SaCheckPermission("realty:commission:approve:manager")
    @Log(title = "佣金主管审批", businessType = BusinessType.UPDATE)
    public Result<Void> approveByManager(@RequestBody CommissionApprovalActionDTO dto) {
        commissionService.approveByManager(dto);
        return Result.ok();
    }

    /**
     * 财务审批
     */
    @PostMapping("/approve/finance")
    @SaCheckPermission("realty:commission:approve:finance")
    @Log(title = "佣金财务审批", businessType = BusinessType.UPDATE)
    public Result<Void> approveByFinance(@RequestBody CommissionApprovalActionDTO dto) {
        commissionService.approveByFinance(dto);
        return Result.ok();
    }

    /**
     * 放款标记
     */
    @PostMapping("/approve/payment")
    @SaCheckPermission("realty:commission:approve:payment")
    @Log(title = "佣金放款", businessType = BusinessType.UPDATE)
    public Result<Void> markPaid(@RequestBody CommissionApprovalActionDTO dto) {
        commissionService.markPaid(dto);
        return Result.ok();
    }

    /**
     * 查询某笔佣金的审批节点记录（用于审批链可视化）
     */
    @GetMapping("/approval/list")
    @SaCheckPermission("realty:commission:list")
    public Result<List<CommissionApproval>> approvalList(@RequestParam Long commissionId) {
        LambdaQueryWrapper<CommissionApproval> wrapper = new LambdaQueryWrapper<CommissionApproval>()
                .eq(CommissionApproval::getCommissionId, commissionId)
                .orderByAsc(CommissionApproval::getApprovalOrder)
                .orderByAsc(CommissionApproval::getApprovalTime);
        return Result.ok(commissionApprovalMapper.selectList(wrapper));
    }

    /**
     * 查询佣金变更日志
     */
    @GetMapping("/changelog")
    public Result<List<CommissionChangeLog>> changeLogs(@RequestParam Long commissionId) {
        return Result.ok(commissionService.getChangeLogs(commissionId));
    }

    /**
     * 检查结佣触发条件
     */
    @GetMapping("/settlement/check")
    public Result<Boolean> checkSettlement(@RequestParam Long dealId) {
        return Result.ok(commissionService.checkSettlementTrigger(dealId));
    }
}
