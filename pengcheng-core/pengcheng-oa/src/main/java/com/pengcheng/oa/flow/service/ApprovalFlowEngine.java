package com.pengcheng.oa.flow.service;

import com.pengcheng.oa.flow.dto.HandleApprovalDTO;
import com.pengcheng.oa.flow.dto.InstanceDetailVO;
import com.pengcheng.oa.flow.dto.StartInstanceDTO;
import com.pengcheng.oa.flow.entity.ApprovalInstance;

import java.util.List;

/**
 * 轻量串行审批流程引擎。
 * <p>
 * 不依赖 Flowable/Activiti，仅支持单级或多级串行审批。
 */
public interface ApprovalFlowEngine {

    /**
     * 启动一个流程实例：根据 flowDefId（或 bizType 的默认流程）
     * 把 currentNodeOrder 设为第 1 个节点；如果流程没有节点，则直接进入终态 APPROVED。
     */
    Long start(StartInstanceDTO dto);

    /**
     * 处理当前节点：通过 / 驳回。
     */
    void handle(HandleApprovalDTO dto);

    /**
     * 申请人取消（仅 RUNNING 时有效）。
     */
    void cancel(Long instanceId, Long applicantId);

    /**
     * 由扫描器/调度器调用：处理超时节点，按节点 timeoutAction 推进。
     * @return 实际推进的实例数
     */
    int sweepTimeouts();

    /**
     * 获取实例详情（含审批记录）。
     */
    InstanceDetailVO detail(Long instanceId);

    /**
     * 获取审批人当前待办（state=RUNNING 且 approverIds 包含 userId）。
     */
    List<ApprovalInstance> listPending(Long approverId);
}
