package com.pengcheng.oa.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程定义（approval_flow_def）。
 * <p>
 * 一个流程定义由若干 {@link ApprovalFlowNode} 节点串行组成，对应同一 bizType。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow_def")
public class ApprovalFlowDef extends BaseEntity {

    /** 业务类型：与 ApprovalTemplate.code 对齐 */
    private String bizType;

    /** 流程名称 */
    private String name;

    /** 是否启用 */
    private Integer enabled;

    /** 是否默认（同一 bizType 仅 1 个 default） */
    private Integer isDefault;

    /** 备注 */
    private String remark;
}
