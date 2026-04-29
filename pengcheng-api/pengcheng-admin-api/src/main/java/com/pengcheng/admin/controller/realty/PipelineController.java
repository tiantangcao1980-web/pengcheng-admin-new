package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.pipeline.dto.OpportunityCreateDTO;
import com.pengcheng.realty.pipeline.dto.OpportunityMoveStageDTO;
import com.pengcheng.realty.pipeline.entity.Opportunity;
import com.pengcheng.realty.pipeline.entity.OpportunityStageLog;
import com.pengcheng.realty.pipeline.entity.PipelineStage;
import com.pengcheng.realty.pipeline.service.PipelineService;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 销售漏斗 + 商机管理控制器
 *
 * 看板视图调用顺序：listStages → 对每个 stage 调 listByStage → 拼装看板
 * 拖拽换阶段：moveStage
 */
@RestController
@RequestMapping("/admin/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    /**
     * 列出所有启用阶段（看板列定义）
     */
    @GetMapping("/stages")
    @SaCheckPermission("realty:pipeline:list")
    public Result<List<PipelineStage>> stages() {
        return Result.ok(pipelineService.listActiveStages());
    }

    /**
     * 创建商机
     */
    @PostMapping("/opportunity")
    @SaCheckPermission("realty:pipeline:create")
    @Log(title = "创建商机", businessType = BusinessType.INSERT)
    public Result<Long> createOpportunity(@RequestBody OpportunityCreateDTO dto) {
        return Result.ok(pipelineService.createOpportunity(dto));
    }

    /**
     * 移动商机到新阶段（拖拽用）
     */
    @PostMapping("/opportunity/move")
    @SaCheckPermission("realty:pipeline:move")
    @Log(title = "商机阶段流转", businessType = BusinessType.UPDATE)
    public Result<Void> moveStage(@RequestBody OpportunityMoveStageDTO dto) {
        pipelineService.moveStage(dto);
        return Result.ok();
    }

    /**
     * 列某阶段下所有商机（看板列内容）
     */
    @GetMapping("/opportunity/by-stage")
    @SaCheckPermission("realty:pipeline:list")
    public Result<List<Opportunity>> listByStage(@RequestParam Long stageId) {
        return Result.ok(pipelineService.listByStage(stageId));
    }

    /**
     * 商机阶段流转日志（详情时间线）
     */
    @GetMapping("/opportunity/{id}/stage-logs")
    @SaCheckPermission("realty:pipeline:list")
    public Result<List<OpportunityStageLog>> stageLogs(@PathVariable("id") Long opportunityId) {
        return Result.ok(pipelineService.getStageLogs(opportunityId));
    }
}
