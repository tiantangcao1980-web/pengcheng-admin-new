package com.pengcheng.finance.commission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通用提成记录实体（commission_record）。
 * <p>
 * 每次触发提成计算产生的记录，支持审核流（待审 → 通过/拒绝 → 发放）。
 */
@Data
@TableName("commission_record")
public class CommissionRecord {

    // ==================== 审核状态常量 ====================
    public static final int AUDIT_PENDING  = 0; // 待审核
    public static final int AUDIT_APPROVED = 1; // 审核通过
    public static final int AUDIT_REJECTED = 2; // 审核拒绝
    public static final int AUDIT_PAID     = 3; // 已发放

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提成规则 ID（commission_rule.id） */
    private Long ruleId;

    /** 销售人员 user_id */
    private Long saleUserId;

    /** 关联业务 ID（如 customer_deal.id / CRM 商机 ID） */
    private Long bizId;

    /** 业务类型（与 rule.bizType 对应） */
    private String bizType;

    /** 提成计算基数（通常为成交金额） */
    private BigDecimal baseAmount;

    /** 应得提成金额 */
    private BigDecimal amount;

    /**
     * 审核状态。
     *
     * @see #AUDIT_PENDING
     * @see #AUDIT_PAID
     */
    private Integer auditStatus;

    /** 审核人 user_id */
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 备注 */
    private String remark;

    /** 创建人 user_id */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
