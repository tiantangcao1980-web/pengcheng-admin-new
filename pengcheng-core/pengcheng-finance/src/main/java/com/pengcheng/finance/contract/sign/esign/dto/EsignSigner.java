package com.pengcheng.finance.contract.sign.esign.dto;

import lombok.Builder;
import lombok.Data;

/**
 * e签宝签署人信息（用于创建签署流时传入的签署方）。
 * <p>
 * 对应 e签宝 API v3 签署流签署人结构。
 * 企业签署使用 {@code orgName} + {@code orgCode}；
 * 个人签署使用 {@code psnName} + {@code psnIdCardNum}。
 */
@Data
@Builder
public class EsignSigner {

    /**
     * 签署方类型。
     * <ul>
     *   <li>PERSONAL — 个人签署</li>
     *   <li>ORGANIZATION — 企业签署</li>
     * </ul>
     */
    private String signerType;

    // ===== 个人签署字段 =====

    /** 姓名（signerType=PERSONAL 时必填） */
    private String psnName;

    /** 手机号（用于发送签署短信通知） */
    private String psnMobile;

    /** 身份证号（用于实名核验，可选） */
    private String psnIdCardNum;

    // ===== 企业签署字段 =====

    /** 企业名称（signerType=ORGANIZATION 时必填） */
    private String orgName;

    /** 统一社会信用代码（signerType=ORGANIZATION 时必填） */
    private String orgCode;

    /** 经办人姓名（企业签署时对接联系人） */
    private String operatorName;

    /** 经办人手机号 */
    private String operatorMobile;

    // ===== 通用字段 =====

    /**
     * 签署顺序（正整数，越小越先签）。
     * 0 或 null 表示不限制顺序（可并行签署）。
     */
    private Integer signOrder;

    /**
     * 本系统签署人 ID（对应 contract_sign_record.signer_id 或外部签署人标识）。
     * 用于回调时关联 sign_record 记录，不传给 e签宝。
     */
    private Long localSignerId;

    /** 签署方角色（partyA/partyB/witness），用于写入 contract_sign_record.signer_role */
    private String signerRole;
}
