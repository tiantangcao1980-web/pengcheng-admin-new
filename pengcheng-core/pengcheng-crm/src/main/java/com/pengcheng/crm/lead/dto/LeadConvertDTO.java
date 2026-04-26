package com.pengcheng.crm.lead.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeadConvertDTO {

    @NotNull
    private Long leadId;

    /** 转化为已存在的客户ID（不传则创建新客户） */
    private Long customerId;

    /** 创建新客户时使用的客户姓名 */
    private String customerName;

    private String remark;
}
