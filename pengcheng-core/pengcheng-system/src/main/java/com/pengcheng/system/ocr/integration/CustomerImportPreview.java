package com.pengcheng.system.ocr.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 名片 OCR 识别结果预览 DTO
 *
 * <p>对应前端"扫名片 → 预填客户表单"场景：
 * <ul>
 *   <li>{@link #name}、{@link #phone}、{@link #email}、{@link #company}、
 *       {@link #position}、{@link #address} — 直接预填到客户创建表单</li>
 *   <li>{@link #telephone} — 座机，供前端展示/手动复制</li>
 *   <li>{@link #website} — 官网，供前端展示</li>
 *   <li>{@link #rawFields} — 原始 OCR 文本行（key=字段标签, value=原始值），
 *       供前端展示置信度或调试用，不入库</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerImportPreview {

    /** 姓名（对应 customer.customer_name） */
    private String name;

    /** 手机号（对应 customer.phone，纯数字） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 公司 */
    private String company;

    /** 职位 */
    private String position;

    /** 地址 */
    private String address;

    /** 座机（辅助展示，不映射到主表） */
    private String telephone;

    /** 网站（辅助展示，不映射到主表） */
    private String website;

    /**
     * 原始 OCR 文本行，供前端展示置信度。
     * key = 字段标签（如 "name"/"phone"/"email"等），value = OCR 原始识别文本。
     */
    private Map<String, String> rawFields;
}
