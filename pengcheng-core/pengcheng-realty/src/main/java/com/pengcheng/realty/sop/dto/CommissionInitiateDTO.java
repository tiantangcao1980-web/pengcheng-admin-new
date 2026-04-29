package com.pengcheng.realty.sop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起佣金三方协议请求 DTO
 * <p>
 * 通常由成交回调自动填充，也可由管理员手动发起。
 */
@Data
public class CommissionInitiateDTO {

    /** 成交单 ID（唯一，防止重复发起） */
    private Long dealId;

    /** 关联带看 SOP ID（用于风控核对，可为 null） */
    private Long visitSopId;

    /** 客户 ID */
    private Long customerId;

    /** 楼盘 ID */
    private Long projectId;

    /** 渠道联盟商 ID */
    private Long allianceId;

    /** 成交金额 */
    private BigDecimal dealAmount;

    /** 佣金费率（如 0.0150 = 1.5%） */
    private BigDecimal commissionRate;

    /** 甲方（开发商）名称 */
    private String partyAName;

    /** 乙方（渠道商）名称 */
    private String partyBName;

    /** 丙方（客户）名称 */
    private String partyCName;

    /** 客户名称（填入协议正文） */
    private String customerName;

    /** 楼盘名称（填入协议正文） */
    private String projectName;

    /** 房源编号（如 1-3-0501） */
    private String fullNo;

    /** 甲方经办人手机（用于 e签宝签署通知） */
    private String partyAMobile;

    /** 乙方经办人手机 */
    private String partyBMobile;

    /** 丙方（客户）手机 */
    private String partyCMobile;

    /** e签宝回调 URL（可选） */
    private String callbackUrl;
}
