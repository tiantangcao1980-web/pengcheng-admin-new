package com.pengcheng.admin.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.common.result.Result;
import com.pengcheng.finance.invoice.entity.Invoice;
import com.pengcheng.finance.invoice.entity.InvoiceItem;
import com.pengcheng.finance.invoice.service.InvoiceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 发票管理 Controller stub（V4 Phase 2 骨架）。
 * <p>
 * 税控/开票 API 对接由 Phase 2 工单完成。
 * URL 前缀 {@code /admin/finance/invoices}。
 */
@RestController
@RequestMapping("/admin/finance/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * 分页查询发票列表。
     *
     * @param customerId 客户 ID（可选）
     * @param status     发票状态（可选）
     * @param pageNum    页码，默认 1
     * @param pageSize   每页条数，默认 10
     */
    @GetMapping
    public Result<IPage<Invoice>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(invoiceService.pageInvoices(customerId, status, pageNum, pageSize));
    }

    /**
     * 查询发票详情（含明细行）。
     *
     * @param id 发票 ID
     */
    @GetMapping("/{id}")
    public Result<InvoiceDetailVO> get(@PathVariable Long id) {
        Invoice invoice = invoiceService.getById(id);
        List<InvoiceItem> items = invoiceService.listItems(id);
        InvoiceDetailVO vo = new InvoiceDetailVO();
        vo.setInvoice(invoice);
        vo.setItems(items);
        return Result.ok(vo);
    }

    /**
     * 申请开票（创建发票 + 明细行，状态=申请中）。
     *
     * @param request 包含发票主表与明细行
     */
    @PostMapping
    public Result<Long> create(@RequestBody InvoiceApplyRequest request) {
        return Result.ok(invoiceService.applyInvoice(request.getInvoice(), request.getItems()));
    }

    /**
     * 更新发票信息（仅审批中前可修改）。
     *
     * @param id      发票 ID
     * @param invoice 更新数据
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Invoice invoice) {
        invoice.setId(id);
        // TODO Phase 2：实现发票信息修改逻辑
        throw new UnsupportedOperationException("Phase 2 待实现：发票信息修改");
    }

    /**
     * 作废发票。
     *
     * @param id 发票 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // TODO Phase 2：从 Sa-Token 获取操作人 ID
        invoiceService.voidInvoice(id, null, "管理员操作");
        return Result.ok();
    }

    // ==================== 内部 VO / Request ====================

    /** 发票详情响应（主表 + 明细行） */
    @Data
    public static class InvoiceDetailVO {
        private Invoice invoice;
        private List<InvoiceItem> items;
    }

    /** 申请开票请求（主表 + 明细行） */
    @Data
    public static class InvoiceApplyRequest {
        private Invoice invoice;
        private List<InvoiceItem> items;
    }
}
