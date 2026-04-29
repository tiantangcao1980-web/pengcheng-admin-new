package com.pengcheng.realty.sop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金三方协议实体（开发商 / 渠道商 / 客户）
 * <p>
 * deal_id 字段设 UNIQUE 约束，天然幂等：同一成交单不能重复发起。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_commission_tripartite")
public class RealtyCommissionTripartite {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成交单 ID（UNIQUE） */
    private Long dealId;

    /** 关联带看 SOP ID（用于风控核对） */
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

    /** 佣金金额 */
    private BigDecimal commissionAmount;

    /** 甲方（开发商）名称 */
    private String partyAName;

    /** 乙方（渠道商）名称 */
    private String partyBName;

    /** 丙方（客户）名称 */
    private String partyCName;

    /** 三方协议文档 URL */
    private String docUrl;

    /** e签宝签署流 ID */
    private String signFlowId;

    /**
     * 签署状态：
     * DRAFT    — 草稿（刚创建）
     * SIGNING  — 签署中
     * SIGNED   — 已全部签署
     * REJECTED — 被拒绝
     * EXPIRED  — 已过期
     */
    private String signStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
