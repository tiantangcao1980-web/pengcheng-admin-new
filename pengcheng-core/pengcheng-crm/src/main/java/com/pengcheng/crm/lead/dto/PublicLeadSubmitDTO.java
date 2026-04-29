package com.pengcheng.crm.lead.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 公开 URL 表单提交 payload。
 * 由 Service 端依据 form schema 校验 fields 内容。
 */
@Data
public class PublicLeadSubmitDTO {

    @NotBlank
    private String formCode;

    /** 与 schema 对应的字段值 */
    private Map<String, Object> fields;
}
