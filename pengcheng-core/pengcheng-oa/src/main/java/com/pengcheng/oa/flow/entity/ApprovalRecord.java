package com.pengcheng.oa.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 单步审批记录（approval_record）。
 * <p>
 * 每位审批人在每个节点的具体操作（通过/驳回/超时跳过）落库一条，
 * 用于详情时间线渲染与回退判定。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_record")
public class ApprovalRecord extends BaseEntity {

    public static final int RESULT_APPROVED = 1;
    public static final int RESULT_REJECTED = 2;
    public static final int RESULT_TIMEOUT_PASS = 3;
    public static final int RESULT_TIMEOUT_REJECT = 4;
    public static final int RESULT_TIMEOUT_SKIP = 5;

    /** 流程实例 ID */
    private Long instanceId;

    /** 节点 ID */
    private Long nodeId;

    /** 节点顺序 */
    private Integer nodeOrder;

    /** 审批人 ID（系统操作时为 null） */
    private Long approverId;

    /** 结果 */
    private Integer result;

    /** 备注 */
    private String remark;

    /** 处理时间 */
    private LocalDateTime actionTime;
}
