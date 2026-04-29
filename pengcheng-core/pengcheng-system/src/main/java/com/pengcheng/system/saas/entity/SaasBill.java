package com.pengcheng.system.saas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("saas_bill")
public class SaasBill implements Serializable {

    public static final String STATUS_UNPAID = "UNPAID";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_OVERDUE = "OVERDUE";
    public static final String STATUS_REFUNDED = "REFUNDED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String billNo;
    private Long tenantId;
    private Long subscriptionId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal baseAmount;
    private BigDecimal overageAmount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createTime;
}
