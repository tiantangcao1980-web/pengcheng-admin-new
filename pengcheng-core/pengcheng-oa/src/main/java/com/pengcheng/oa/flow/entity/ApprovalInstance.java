package com.pengcheng.oa.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批流程实例（approval_instance）。
 * <p>
 * 一次具体的申请，承载流程的运行时状态：当前节点、状态、终态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_instance")
public class ApprovalInstance extends BaseEntity {

    public static final int STATE_RUNNING = 1;
    public static final int STATE_APPROVED = 2;
    public static final int STATE_REJECTED = 3;
    public static final int STATE_CANCELLED = 4;

    /** 流程定义 ID */
    private Long flowDefId;

    /** 业务类型 */
    private String bizType;

    /** 业务实体 ID */
    private Long bizId;

    /** 申请人 ID */
    private Long applicantId;

    /** 当前节点顺序（nodeOrder） */
    private Integer currentNodeOrder;

    /** 当前节点 ID */
    private Long currentNodeId;

    /** 状态 1=运行中 2=通过 3=驳回 4=撤销 */
    private Integer state;

    /** 摘要 */
    private String summary;

    /** 当前节点超时时间（用于扫描器判定超时） */
    private LocalDateTime currentNodeDeadline;

    /** 流程结束时间 */
    private LocalDateTime endTime;
}
