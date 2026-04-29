package com.pengcheng.realty.commission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 佣金审批节点记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("commission_approval")
public class CommissionApproval implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 佣金ID */
    private Long commissionId;

    /** 审批节点：MANAGER / FINANCE / PAYMENT */
    private String node;

    /** 审批人ID */
    private Long approverId;

    /** 审批结果：1-通过 2-驳回 */
    private Integer result;

    /** 审批备注（驳回时建议必填） */
    private String remark;

    /** 审批顺序：1=主管 2=财务 3=放款 */
    private Integer approvalOrder;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 审批结果常量 */
    public static final int RESULT_APPROVED = 1;
    public static final int RESULT_REJECTED = 2;

    /** 节点常量 */
    public static final String NODE_MANAGER = "MANAGER";
    public static final String NODE_FINANCE = "FINANCE";
    public static final String NODE_PAYMENT = "PAYMENT";

    /** 审批顺序常量 */
    public static final int ORDER_MANAGER = 1;
    public static final int ORDER_FINANCE = 2;
    public static final int ORDER_PAYMENT = 3;
}
