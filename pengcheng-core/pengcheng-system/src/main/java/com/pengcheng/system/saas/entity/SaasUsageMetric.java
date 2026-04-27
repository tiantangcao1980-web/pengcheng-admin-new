package com.pengcheng.system.saas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("saas_usage_metric")
public class SaasUsageMetric implements Serializable {

    public static final String TYPE_MAU = "MAU";
    public static final String TYPE_API_CALLS = "API_CALLS";
    public static final String TYPE_STORAGE_GB = "STORAGE_GB";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String metricType;
    private String periodYyyymm;
    private Long valueNum;
    private LocalDateTime lastUpdate;
}
