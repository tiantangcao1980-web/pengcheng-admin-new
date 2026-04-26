package com.pengcheng.oa.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程节点（approval_flow_node）。
 * <p>
 * 节点按 nodeOrder 升序串行执行；每个节点指定一个或多个审批人（approverIds，逗号分隔的用户 ID）。
 * 任一审批人通过即可推进；任一审批人驳回则整个流程驳回。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow_node")
public class ApprovalFlowNode extends BaseEntity {

    /** 节点类型 */
    public static final int NODE_TYPE_USER = 1;
    public static final int NODE_TYPE_DEPT_HEAD = 2;
    public static final int NODE_TYPE_ROLE = 3;

    /** 流程定义 ID */
    private Long flowDefId;

    /** 节点顺序，从 1 开始 */
    private Integer nodeOrder;

    /** 节点名称 */
    private String nodeName;

    /** 节点类型 1=指定用户 2=部门主管 3=角色 */
    private Integer nodeType;

    /** 审批人 ID 列表（逗号分隔，nodeType=1 时使用） */
    private String approverIds;

    /** 角色 KEY（nodeType=3 时使用） */
    private String roleKey;

    /** 超时小时数（>=0 时启用），0/null = 不超时 */
    private Integer timeoutHours;

    /** 超时策略 1=自动通过 2=自动驳回 3=跳过 */
    private Integer timeoutAction;

    /** 是否允许加签 */
    private Integer allowAddSign;
}
