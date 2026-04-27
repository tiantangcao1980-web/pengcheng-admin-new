package com.pengcheng.admin.controller.oa;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.oa.flow.dto.HandleApprovalDTO;
import com.pengcheng.oa.flow.dto.InstanceDetailVO;
import com.pengcheng.oa.flow.dto.StartInstanceDTO;
import com.pengcheng.oa.flow.entity.ApprovalFlowDef;
import com.pengcheng.oa.flow.entity.ApprovalFlowNode;
import com.pengcheng.oa.flow.entity.ApprovalInstance;
import com.pengcheng.oa.flow.service.ApprovalFlowDefService;
import com.pengcheng.oa.flow.service.ApprovalFlowEngine;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.annotation.RepeatSubmit;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * V4 MVP 闭环② — 轻量串行审批流程 Controller。
 * URL 与 pengcheng-ui/src/api/oaApprovalFlow.ts 对齐。
 */
@RestController
@RequestMapping("/admin/oa/approval-flow")
@RequiredArgsConstructor
public class OaApprovalFlowController {

    private final ApprovalFlowDefService defService;
    private final ApprovalFlowEngine flowEngine;

    /* ========== 流程定义 CRUD ========== */

    @GetMapping("/defs")
    @SaCheckPermission("oa:approval-flow:list")
    public Result<List<ApprovalFlowDef>> listDefs(@RequestParam(required = false) String bizType) {
        return Result.ok(bizType != null ? defService.listByBizType(bizType) : defService.listAll());
    }

    @GetMapping("/defs/{id}")
    @SaCheckPermission("oa:approval-flow:list")
    public Result<FlowDefPayload> getDef(@PathVariable Long id) {
        ApprovalFlowDef def = defService.getDef(id);
        List<ApprovalFlowNode> nodes = defService.listNodes(id);
        return Result.ok(new FlowDefPayload(def, nodes));
    }

    @PostMapping("/defs")
    @SaCheckPermission("oa:approval-flow:add")
    @RepeatSubmit
    @Log(title = "审批流程定义", businessType = BusinessType.INSERT)
    public Result<Long> createDef(@RequestBody FlowDefPayload payload) {
        return Result.ok(defService.createDef(payload.getDef(), payload.getNodes()));
    }

    @PutMapping("/defs/{id}")
    @SaCheckPermission("oa:approval-flow:edit")
    @Log(title = "审批流程定义", businessType = BusinessType.UPDATE)
    public Result<Void> updateDef(@PathVariable Long id, @RequestBody FlowDefPayload payload) {
        if (payload.getDef() != null) {
            payload.getDef().setId(id);
        }
        defService.updateDef(payload.getDef(), payload.getNodes());
        return Result.ok();
    }

    @DeleteMapping("/defs/{id}")
    @SaCheckPermission("oa:approval-flow:delete")
    @Log(title = "审批流程定义", businessType = BusinessType.DELETE)
    public Result<Void> deleteDef(@PathVariable Long id) {
        defService.deleteDef(id);
        return Result.ok();
    }

    /* ========== 流程实例：启动 / 处理 / 撤销 / 待办 ========== */

    @PostMapping("/instances")
    @SaCheckPermission("oa:approval-flow:add")
    @RepeatSubmit
    @Log(title = "审批实例", businessType = BusinessType.INSERT)
    public Result<Long> startInstance(@RequestBody StartInstanceDTO dto) {
        if (dto.getApplicantId() == null) {
            dto.setApplicantId(StpUtil.getLoginIdAsLong());
        }
        return Result.ok(flowEngine.start(dto));
    }

    @GetMapping("/instances/pending")
    @SaCheckPermission("oa:approval-flow:list")
    public Result<List<ApprovalInstance>> listPending(@RequestParam(required = false) Long approverId) {
        Long target = (approverId != null) ? approverId : StpUtil.getLoginIdAsLong();
        return Result.ok(flowEngine.listPending(target));
    }

    @GetMapping("/instances/{id}")
    @SaCheckPermission("oa:approval-flow:list")
    public Result<InstanceDetailVO> getInstance(@PathVariable Long id) {
        return Result.ok(flowEngine.detail(id));
    }

    @PostMapping("/instances/handle")
    @SaCheckPermission("oa:approval-flow:approve")
    @Log(title = "审批处理", businessType = BusinessType.UPDATE)
    public Result<Void> handle(@RequestBody HandleApprovalDTO dto) {
        if (dto.getApproverId() == null) {
            dto.setApproverId(StpUtil.getLoginIdAsLong());
        }
        flowEngine.handle(dto);
        return Result.ok();
    }

    @PostMapping("/instances/cancel")
    @SaCheckPermission("oa:approval-flow:edit")
    @Log(title = "审批撤销", businessType = BusinessType.UPDATE)
    public Result<Void> cancel(@RequestBody CancelInstanceRequest request) {
        Long applicantId = request.getApplicantId() != null
                ? request.getApplicantId()
                : StpUtil.getLoginIdAsLong();
        flowEngine.cancel(request.getInstanceId(), applicantId);
        return Result.ok();
    }

    /** 流程定义 + 节点的复合载荷。 */
    @Data
    public static class FlowDefPayload {
        private ApprovalFlowDef def;
        private List<ApprovalFlowNode> nodes;

        public FlowDefPayload() {}

        public FlowDefPayload(ApprovalFlowDef def, List<ApprovalFlowNode> nodes) {
            this.def = def;
            this.nodes = nodes;
        }
    }

    /** 撤销请求载荷。 */
    @Data
    public static class CancelInstanceRequest {
        private Long instanceId;
        private Long applicantId;
    }
}
