package com.pengcheng.finance.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 合同主表实体（contract）。
 * <p>
 * 生命周期：起草 → 审批 → 签署 → 履约 → 归档/作废。
 * <p>
 * 注意：{@code customer_deal.contract_no} 字段为 realty 模块的旧字段，保持不变；
 * 本表 {@code contract_no} 是财务闭环统一合同号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {

    // ==================== 合同状态常量 ====================
    public static final int STATUS_DRAFT      = 1;  // 起草
    public static final int STATUS_APPROVING  = 2;  // 审批中
    public static final int STATUS_APPROVED   = 3;  // 审批通过
    public static final int STATUS_REJECTED   = 4;  // 审批拒绝
    public static final int STATUS_SIGNING    = 5;  // 签署中
    public static final int STATUS_SIGNED     = 6;  // 签署完成
    public static final int STATUS_PERFORMING = 7;  // 履约中
    public static final int STATUS_ARCHIVED   = 8;  // 已归档
    public static final int STATUS_VOID       = 9;  // 已作废

    // ==================== 签署状态常量 ====================
    public static final int SIGN_STATUS_NONE    = 0; // 未签署
    public static final int SIGN_STATUS_PARTIAL = 1; // 部分签署
    public static final int SIGN_STATUS_ALL     = 2; // 全部签署

    /** 合同编号，系统生成，全局唯一 */
    private String contractNo;

    /** 合同标题 */
    private String title;

    /** 来源模板 ID（contract_template.id，自由录入时为 null） */
    private Long templateId;

    /** 关联客户 ID（customer.id） */
    private Long customerId;

    /** 关联成交 ID（customer_deal.id） */
    private Long dealId;

    /** 合同金额 */
    private BigDecimal amount;

    /**
     * 合同状态。
     *
     * @see #STATUS_DRAFT
     * @see #STATUS_SIGNED
     * @see #STATUS_ARCHIVED
     */
    private Integer status;

    /**
     * 签署状态。
     *
     * @see #SIGN_STATUS_NONE
     * @see #SIGN_STATUS_ALL
     */
    private Integer signStatus;

    /** 电子签服务商：esign / fadada / offline */
    private String signProvider;

    /** 外部签署平台合同 ID（e签宝 flowId 等，Phase 2 对接后填充） */
    private String externalSignId;

    /** 当前版本号 */
    private Integer version;

    /** 备注 */
    private String remark;
}
