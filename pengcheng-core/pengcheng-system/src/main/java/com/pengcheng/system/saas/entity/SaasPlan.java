package com.pengcheng.system.saas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("saas_plan")
public class SaasPlan implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;
    private String name;
    private BigDecimal pricePerMonth;
    private Integer maxUsers;
    private Integer maxStorageGb;
    private Integer maxApiCallsPerMonth;
    private String features;
    private Integer enabled;
    private LocalDateTime createTime;
}
