package com.pengcheng.realty.commission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金主表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("commission")
public class Commission extends BaseEntity {

    /**
     * 关联成交记录ID
     */
    private Long dealId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 联盟商ID
     */
    private Long allianceId;

    /**
     * 应收佣金
     */
    private BigDecimal receivableAmount;

    /**
     * 应结佣金
     */
    private BigDecimal payableAmount;

    /**
     * 公司平台费
     */
    private BigDecimal platformFee;

    /**
     * 物业类型：RESIDENTIAL住宅 COMMERCIAL商铺 APARTMENT公寓 OFFICE写字楼 OTHER其他（V17 新增）
     * 与 ProjectCommissionRule.propertyType 联动决定佣金率
     */
    private String propertyType;

    /**
     * 客户籍贯：DOMESTIC内地 OVERSEAS境外（V17 新增）
     * 与 ProjectCommissionRule.customerOrigin 联动决定佣金率
     */
    private String customerOrigin;

    /**
     * 审核状态（旧字段，兼容存量数据与旧 API）：1-待审核 2-审核通过 3-审核驳回
     */
    private Integer auditStatus;

    /**
     * 审核备注（旧字段，承载最近一次审批的备注）
     */
    private String auditRemark;

    /**
     * 审核人ID（旧字段，承载最近一次审批人）
     */
    private Long auditorId;

    /**
     * 审核时间（旧字段，承载最近一次审批时间）
     */
    private LocalDateTime auditTime;

    /**
     * 当前审批节点：DRAFT / SUBMITTED / MANAGER_APPROVED / FINANCE_APPROVED / PAID / REJECTED
     */
    private String approvalNode;

    /**
     * 提交人ID（业务员）
     */
    private Long submittedBy;

    /**
     * 提交时间
     */
    private LocalDateTime submittedTime;

    /**
     * 放款操作人ID
     */
    private Long paidBy;

    /**
     * 放款时间
     */
    private LocalDateTime paidTime;
}
