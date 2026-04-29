package com.pengcheng.oa.flow.service;

import com.pengcheng.oa.flow.entity.ApprovalFlowDef;
import com.pengcheng.oa.flow.entity.ApprovalFlowNode;

import java.util.List;

public interface ApprovalFlowDefService {

    Long createDef(ApprovalFlowDef def, List<ApprovalFlowNode> nodes);

    void updateDef(ApprovalFlowDef def, List<ApprovalFlowNode> nodes);

    void deleteDef(Long id);

    ApprovalFlowDef getDef(Long id);

    List<ApprovalFlowNode> listNodes(Long defId);

    List<ApprovalFlowDef> listByBizType(String bizType);

    List<ApprovalFlowDef> listAll();
}
