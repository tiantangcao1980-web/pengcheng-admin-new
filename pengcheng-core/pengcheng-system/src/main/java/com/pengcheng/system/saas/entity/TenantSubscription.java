package com.pengcheng.system.saas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tenant_subscription")
public class TenantSubscription implements Serializable {

    public static final String STATUS_TRIAL = "TRIAL";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long planId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer autoRenew;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
