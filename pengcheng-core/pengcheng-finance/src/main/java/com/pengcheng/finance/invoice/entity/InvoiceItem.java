package com.pengcheng.finance.invoice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发票明细行实体（invoice_item）。
 * <p>
 * 对应发票"货物或服务清单"，一张发票可有多行明细。
 */
@Data
@TableName("invoice_item")
public class InvoiceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发票 ID（invoice.id） */
    private Long invoiceId;

    /** 项目名称 / 货物名称 */
    private String itemName;

    /** 规格型号 */
    private String spec;

    /** 计量单位 */
    private String unit;

    /** 数量 */
    private BigDecimal qty;

    /** 单价（不含税） */
    private BigDecimal unitPrice;

    /** 金额（不含税） */
    private BigDecimal amount;

    /** 行序号 */
    private Integer sort;
}
