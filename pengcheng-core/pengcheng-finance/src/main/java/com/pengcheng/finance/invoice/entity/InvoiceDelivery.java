package com.pengcheng.finance.invoice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发票物流记录实体（invoice_delivery）。
 * <p>
 * 记录发票快递寄送与客户签收状态，支持物流追踪。
 */
@Data
@TableName("invoice_delivery")
public class InvoiceDelivery {

    // ==================== 签收状态常量 ====================
    public static final int SIGN_STATUS_UNSENT   = 0; // 未寄出
    public static final int SIGN_STATUS_TRANSIT  = 1; // 运输中
    public static final int SIGN_STATUS_SIGNED   = 2; // 已签收
    public static final int SIGN_STATUS_ABNORMAL = 3; // 签收异常

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发票 ID（invoice.id） */
    private Long invoiceId;

    /** 快递公司（如 SF / YTO / ZTO） */
    private String expressProvider;

    /** 快递单号 */
    private String expressNo;

    /** 寄出时间 */
    private LocalDateTime sendTime;

    /**
     * 签收状态。
     *
     * @see #SIGN_STATUS_UNSENT
     * @see #SIGN_STATUS_SIGNED
     */
    private Integer signStatus;

    /** 客户签收时间 */
    private LocalDateTime signTime;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件地址 */
    private String receiverAddress;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
