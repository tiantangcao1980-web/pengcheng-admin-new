package com.pengcheng.admin.controller.realty;

import com.pengcheng.common.result.PageResult;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.payment.dto.PaymentApprovalDTO;
import com.pengcheng.realty.payment.dto.PaymentQueryDTO;
import com.pengcheng.realty.payment.dto.PaymentRequestDTO;
import com.pengcheng.realty.payment.dto.PaymentVO;
import com.pengcheng.realty.payment.entity.PaymentApproval;
import com.pengcheng.realty.payment.service.PaymentService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 付款申请管理控制器
 */
@RestController
@RequestMapping("/admin/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建付款申请
     */
    @PostMapping("/create")
    @Log(title = "付款申请", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody PaymentRequestDTO dto) {
        return Result.ok(paymentService.createPaymentRequest(dto));
    }

    /**
     * 审批付款申请
     */
    @PostMapping("/approve")
    @Log(title = "付款审批", businessType = BusinessType.UPDATE)
    public Result<Void> approve(@RequestBody PaymentApprovalDTO dto) {
        paymentService.approvePaymentRequest(dto);
        return Result.ok();
    }

    /**
     * 分页查询付款申请
     */
    @GetMapping("/page")
    public Result<PageResult<PaymentVO>> page(PaymentQueryDTO query) {
        return Result.ok(paymentService.pagePaymentRequests(query));
    }

    /**
     * 查询审批流转历史
     */
    @GetMapping("/approvals")
    public Result<List<PaymentApproval>> approvals(@RequestParam Long requestId) {
        return Result.ok(paymentService.getApprovalHistory(requestId));
    }
}
