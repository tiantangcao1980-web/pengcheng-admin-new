package com.pengcheng.finance.invoice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发票主表实体（invoice）。
 * <p>
 * 覆盖申请 → 审批 → 开具的全流程主记录。
 * 税控/开票 API 对接（诺诺/百望云等）留给 Phase 2 后续工单实现。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice")
public class Invoice extends BaseEntity {

    // ==================== 发票类型常量 ====================
    public static final int TYPE_VAT_NORMAL   = 1; // 增值税普票
    public static final int TYPE_VAT_SPECIAL  = 2; // 增值税专票
    public static final int TYPE_ELEC_NORMAL  = 3; // 电子普票
    public static final int TYPE_ELEC_SPECIAL = 4; // 电子专票

    // ==================== 发票状态常量 ====================
    public static final int STATUS_APPLYING  = 1; // 申请中
    public static final int STATUS_APPROVED  = 2; // 审批通过
    public static final int STATUS_REJECTED  = 3; // 审批拒绝
    public static final int STATUS_ISSUED    = 4; // 已开具
    public static final int STATUS_VOIDED    = 5; // 已作废
    public static final int STATUS_RED       = 6; // 已红冲

    /** 发票号码（开具后由税控系统返回填充） */
    private String invoiceNo;

    /** 关联合同 ID（contract.id，允许为空） */
    private Long contractId;

    /** 购买方客户 ID（customer.id） */
    private Long customerId;

    /** 开票金额（不含税） */
    private BigDecimal amount;

    /** 税率（如 0.0600 = 6%） */
    private BigDecimal taxRate;

    /** 税额（系统计算：amount * taxRate） */
    private BigDecimal taxAmount;

    /** 价税合计 */
    private BigDecimal totalAmount;

    /**
     * 发票类型。
     *
     * @see #TYPE_VAT_NORMAL
     * @see #TYPE_ELEC_SPECIAL
     */
    private Integer invoiceType;

    /**
     * 发票状态。
     *
     * @see #STATUS_APPLYING
     * @see #STATUS_ISSUED
     */
    private Integer status;

    /** 开票日期 */
    private LocalDate issueDate;

    /** 开票人 user_id */
    private Long issuerId;

    /** 备注 */
    private String remark;
}
