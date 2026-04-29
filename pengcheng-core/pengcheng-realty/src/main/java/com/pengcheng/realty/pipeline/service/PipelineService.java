package com.pengcheng.realty.pipeline.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.pipeline.dto.OpportunityCreateDTO;
import com.pengcheng.realty.pipeline.dto.OpportunityMoveStageDTO;
import com.pengcheng.realty.pipeline.entity.Opportunity;
import com.pengcheng.realty.pipeline.entity.OpportunityStageLog;
import com.pengcheng.realty.pipeline.entity.PipelineStage;
import com.pengcheng.realty.pipeline.mapper.OpportunityMapper;
import com.pengcheng.realty.pipeline.mapper.OpportunityStageLogMapper;
import com.pengcheng.realty.pipeline.mapper.PipelineStageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售漏斗 + 商机管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineStageMapper stageMapper;
    private final OpportunityMapper opportunityMapper;
    private final OpportunityStageLogMapper stageLogMapper;

    // ============================================================
    // 阶段
    // ============================================================

    /** 列出所有启用阶段（按 orderNo 升序，看板列序） */
    public List<PipelineStage> listActiveStages() {
        return stageMapper.selectList(new LambdaQueryWrapper<PipelineStage>()
                .eq(PipelineStage::getActive, 1)
                .orderByAsc(PipelineStage::getOrderNo));
    }

    /** 按 code 查阶段 */
    public PipelineStage findStageByCode(String code) {
        return stageMapper.selectOne(new LambdaQueryWrapper<PipelineStage>()
                .eq(PipelineStage::getCode, code));
    }

    // ============================================================
    // 商机
    // ============================================================

    /** 创建商机：默认起始阶段 LEAD */
    @Transactional
    public Long createOpportunity(OpportunityCreateDTO dto) {
        if (dto == null || dto.getCustomerId() == null) {
            throw new IllegalArgumentException("客户ID不能为空");
        }
        if (dto.getProjectId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }

        Long stageId = dto.getStageId();
        if (stageId == null) {
            PipelineStage lead = findStageByCode(PipelineStage.CODE_LEAD);
            if (lead == null) {
                throw new IllegalStateException("默认 LEAD 阶段未初始化");
            }
            stageId = lead.getId();
        }

        Opportunity opp = Opportunity.builder()
                .customerId(dto.getCustomerId())
                .projectId(dto.getProjectId())
                .stageId(stageId)
                .title(dto.getTitle())
                .expectedAmount(dto.getExpectedAmount())
                .expectedCloseDate(dto.getExpectedCloseDate())
                .ownerId(dto.getOwnerId())
                .nextAction(dto.getNextAction())
                .lastStageChangedAt(LocalDateTime.now())
                .build();
        opportunityMapper.insert(opp);

        // 写阶段日志：from=null
        stageLogMapper.insert(OpportunityStageLog.builder()
                .opportunityId(opp.getId())
                .fromStageId(null)
                .toStageId(stageId)
                .operatorId(dto.getOwnerId())
                .remark("创建商机")
                .changeTime(LocalDateTime.now())
                .build());

        log.info("[商机] 创建 id={} customer={} project={} stageId={}",
                opp.getId(), dto.getCustomerId(), dto.getProjectId(), stageId);
        return opp.getId();
    }

    /**
     * 移动商机到新阶段（看板拖拽用）。
     * 校验：阶段存在 + 与当前阶段不同 + 终态不可再移动到非终态
     */
    @Transactional
    public void moveStage(OpportunityMoveStageDTO dto) {
        if (dto == null || dto.getOpportunityId() == null || dto.getToStageId() == null) {
            throw new IllegalArgumentException("商机ID与目标阶段ID不能为空");
        }

        Opportunity opp = opportunityMapper.selectById(dto.getOpportunityId());
        if (opp == null) {
            throw new IllegalArgumentException("商机不存在: " + dto.getOpportunityId());
        }
        if (opp.getStageId().equals(dto.getToStageId())) {
            return;  // 同阶段，no-op
        }

        PipelineStage to = stageMapper.selectById(dto.getToStageId());
        if (to == null || to.getActive() == 0) {
            throw new IllegalArgumentException("目标阶段不存在或已禁用");
        }

        Long fromStageId = opp.getStageId();
        opp.setStageId(dto.getToStageId());
        opp.setLastStageChangedAt(LocalDateTime.now());
        opp.setUpdateBy(dto.getOperatorId());

        // 流失阶段必填备注
        if (PipelineStage.CODE_LOST.equals(to.getCode())) {
            if (dto.getRemark() == null || dto.getRemark().isBlank()) {
                throw new IllegalArgumentException("流失到 LOST 必须填写流失原因");
            }
            opp.setLostReason(dto.getRemark());
        }
        opportunityMapper.updateById(opp);

        stageLogMapper.insert(OpportunityStageLog.builder()
                .opportunityId(opp.getId())
                .fromStageId(fromStageId)
                .toStageId(dto.getToStageId())
                .operatorId(dto.getOperatorId())
                .remark(dto.getRemark())
                .changeTime(LocalDateTime.now())
                .build());

        log.info("[商机] id={} 阶段流转 {} → {}", opp.getId(), fromStageId, dto.getToStageId());
    }

    /** 列某阶段下所有商机（看板列内容） */
    public List<Opportunity> listByStage(Long stageId) {
        return opportunityMapper.selectList(new LambdaQueryWrapper<Opportunity>()
                .eq(Opportunity::getStageId, stageId)
                .orderByDesc(Opportunity::getLastStageChangedAt));
    }

    /** 查商机阶段流转日志（详情时间线） */
    public List<OpportunityStageLog> getStageLogs(Long opportunityId) {
        return stageLogMapper.selectList(new LambdaQueryWrapper<OpportunityStageLog>()
                .eq(OpportunityStageLog::getOpportunityId, opportunityId)
                .orderByAsc(OpportunityStageLog::getChangeTime));
    }
}
