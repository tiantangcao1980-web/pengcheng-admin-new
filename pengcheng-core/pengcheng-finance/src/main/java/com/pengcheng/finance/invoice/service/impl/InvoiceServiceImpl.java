package com.pengcheng.finance.invoice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.invoice.entity.Invoice;
import com.pengcheng.finance.invoice.entity.InvoiceDelivery;
import com.pengcheng.finance.invoice.entity.InvoiceItem;
import com.pengcheng.finance.invoice.mapper.InvoiceDeliveryMapper;
import com.pengcheng.finance.invoice.mapper.InvoiceItemMapper;
import com.pengcheng.finance.invoice.mapper.InvoiceMapper;
import com.pengcheng.finance.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 发票开票闭环服务实现（Phase 2 骨架占位）。
 * <p>
 * 税控/开票 API（诺诺/百望云）对接由 Phase 2 工单落地。
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceMapper invoiceMapper;
    private final InvoiceItemMapper invoiceItemMapper;
    private final InvoiceDeliveryMapper invoiceDeliveryMapper;

    @Override
    public Long applyInvoice(Invoice invoice, List<InvoiceItem> itemList) {
        throw new UnsupportedOperationException("Phase 2 待实现：发票申请");
    }

    @Override
    public void approveInvoice(Long invoiceId, Long operatorId) {
        throw new UnsupportedOperationException("Phase 2 待实现：发票审批通过");
    }

    @Override
    public void rejectInvoice(Long invoiceId, Long operatorId, String reason) {
        throw new UnsupportedOperationException("Phase 2 待实现：发票审批拒绝");
    }

    @Override
    public void issueInvoice(Long invoiceId, Long operatorId) {
        // TODO Phase 2：调用诺诺/百望云税控 API 开具发票，填充 invoice_no 和 issue_date
        throw new UnsupportedOperationException("Phase 2 待实现：税控开票 API 调用");
    }

    @Override
    public void voidInvoice(Long invoiceId, Long operatorId, String reason) {
        throw new UnsupportedOperationException("Phase 2 待实现：发票作废");
    }

    @Override
    public void redFlushInvoice(Long invoiceId, Long operatorId) {
        // TODO Phase 2：调用税控 API 红冲，生成红字发票
        throw new UnsupportedOperationException("Phase 2 待实现：红冲发票（税控 API）");
    }

    @Override
    public Long recordDelivery(InvoiceDelivery delivery) {
        throw new UnsupportedOperationException("Phase 2 待实现：登记发票快递");
    }

    @Override
    public void confirmSigned(Long deliveryId, Long operatorId) {
        throw new UnsupportedOperationException("Phase 2 待实现：确认客户签收");
    }

    @Override
    public Invoice getById(Long id) {
        throw new UnsupportedOperationException("Phase 2 待实现：发票详情查询");
    }

    @Override
    public List<InvoiceItem> listItems(Long invoiceId) {
        throw new UnsupportedOperationException("Phase 2 待实现：发票明细查询");
    }

    @Override
    public IPage<Invoice> pageInvoices(Long customerId, Integer status, int pageNum, int pageSize) {
        throw new UnsupportedOperationException("Phase 2 待实现：发票分页列表");
    }
}
