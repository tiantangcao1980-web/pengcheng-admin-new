package com.pengcheng.crm.customfield.entity;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "custom_field_value", autoResultMap = true)
public class CustomFieldValue implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String entityType;
    private Long entityId;
    private Long fieldId;
    private String fieldKey;

    private String valueText;
    private BigDecimal valueNumber;
    private LocalDateTime valueDate;

    /** 多选/文件等 JSON */
    private String valueJson;

    private Long tenantId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
