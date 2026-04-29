package com.pengcheng.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 公开线索采集表单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "crm_lead_form", autoResultMap = true)
public class CrmLeadForm extends BaseEntity {

    private String formCode;
    private String title;
    private String description;

    /** 字段定义 JSON 串 */
    private String schemaJson;

    private Long defaultOwnerId;
    private String defaultSource;
    private String qrcodeUrl;

    private Integer enabled;
    private Integer submitCount;
    private Long tenantId;
}
