package com.pengcheng.finance.invoice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.invoice.entity.Invoice;
import com.pengcheng.finance.invoice.entity.InvoiceDelivery;
import com.pengcheng.finance.invoice.entity.InvoiceItem;

import java.util.List;

/**
 * 发票开票闭环服务接口。
 * <p>
 * 覆盖：申请 → 审批 → 开具（税控 API，Phase 2 对接）→ 快递寄送 → 客户签收。
 */
public interface InvoiceService {

    /**
     * 申请开票（状态=申请中）。
     * <p>
     * 同时传入明细行，税额由服务自动计算（amount * taxRate）。
     *
     * @param invoice   发票主表数据
     * @param itemList  明细行列表（至少一行）
     * @return 新发票 ID
     */
    Long applyInvoice(Invoice invoice, List<InvoiceItem> itemList);

    /**
     * 审批通过（状态变为审批通过）。
     *
     * @param invoiceId  发票 ID
     * @param operatorId 审批人 user_id
     */
    void approveInvoice(Long invoiceId, Long operatorId);

    /**
     * 审批拒绝（状态变为审批拒绝）。
     *
     * @param invoiceId  发票 ID
     * @param operatorId 审批人 user_id
     * @param reason     拒绝原因
     */
    void rejectInvoice(Long invoiceId, Long operatorId, String reason);

    /**
     * 调用税控/开票 API 实际开具发票（填充 invoiceNo、issueDate）。
     * <p>
     * TODO Phase 2：对接诺诺/百望云税控开票 API。
     *
     * @param invoiceId  发票 ID
     * @param operatorId 开票人 user_id
     * @throws UnsupportedOperationException 税控开票 API 对接 Phase 2 待实现
     */
    void issueInvoice(Long invoiceId, Long operatorId);

    /**
     * 作废发票（状态变为已作废）。
     *
     * @param invoiceId  发票 ID
     * @param operatorId 操作人 user_id
     * @param reason     作废原因
     */
    void voidInvoice(Long invoiceId, Long operatorId, String reason);

    /**
     * 红冲发票（状态变为已红冲）。
     *
     * @param invoiceId  原发票 ID
     * @param operatorId 操作人 user_id
     * @throws UnsupportedOperationException 红冲 API Phase 2 待实现
     */
    void redFlushInvoice(Long invoiceId, Long operatorId);

    /**
     * 登记快递寄送信息（创建物流记录）。
     *
     * @param delivery 物流数据（invoiceId、expressProvider、expressNo 必填）
     * @return 物流记录 ID
     */
    Long recordDelivery(InvoiceDelivery delivery);

    /**
     * 确认客户签收（更新物流状态为已签收）。
     *
     * @param deliveryId 物流记录 ID
     * @param operatorId 操作人 user_id
     */
    void confirmSigned(Long deliveryId, Long operatorId);

    /**
     * 按 ID 查询发票详情。
     *
     * @param id 发票 ID
     * @return 发票实体；不存在时返回 null
     */
    Invoice getById(Long id);

    /**
     * 查询发票明细行列表。
     *
     * @param invoiceId 发票 ID
     * @return 明细行列表（按 sort 升序）
     */
    List<InvoiceItem> listItems(Long invoiceId);

    /**
     * 分页查询发票列表。
     *
     * @param customerId 客户 ID 过滤（可为 null）
     * @param status     发票状态过滤（可为 null）
     * @param pageNum    页码（从 1 开始）
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<Invoice> pageInvoices(Long customerId, Integer status, int pageNum, int pageSize);
}
