package com.pengcheng.system.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OCR 名片识别结果
 *
 * <p>字段对齐 {@code crm_customer} 表，便于直接预填客户表单。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessCardData {

    private String name;

    /** 公司 / 单位 */
    private String company;

    /** 职位 */
    private String position;

    /** 手机号（已规范化为纯数字） */
    private String mobile;

    /** 座机 */
    private String telephone;

    /** 邮箱（小写） */
    private String email;

    /** 地址 */
    private String address;

    /** 网站 */
    private String website;

    /** 原始 OCR 文本（debug 用，不入库） */
    private String rawText;
}
