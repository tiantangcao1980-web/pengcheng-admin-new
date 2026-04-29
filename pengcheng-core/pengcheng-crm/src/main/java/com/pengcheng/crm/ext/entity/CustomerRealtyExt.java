package com.pengcheng.crm.ext.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户·房产行业扩展（1:1 关联 customer.id）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer_realty_ext")
public class CustomerRealtyExt implements Serializable {

    /** 主键 = customer.id */
    @TableId(type = IdType.INPUT)
    private Long customerId;

    private Integer visitCount;
    private LocalDateTime visitTime;
    private Long allianceId;
    private String agentName;
    private String agentPhone;
    private BigDecimal dealProbability;
    private LocalDateTime protectionExpireTime;
    private String reportNo;
    private Long tenantId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
