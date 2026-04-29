package com.pengcheng.realty.pipeline.service;

import com.pengcheng.realty.pipeline.dto.OpportunityCreateDTO;
import com.pengcheng.realty.pipeline.dto.OpportunityMoveStageDTO;
import com.pengcheng.realty.pipeline.entity.Opportunity;
import com.pengcheng.realty.pipeline.entity.OpportunityStageLog;
import com.pengcheng.realty.pipeline.entity.PipelineStage;
import com.pengcheng.realty.pipeline.mapper.OpportunityMapper;
import com.pengcheng.realty.pipeline.mapper.OpportunityStageLogMapper;
import com.pengcheng.realty.pipeline.mapper.PipelineStageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售漏斗 + 商机服务单测
 */
@DisplayName("PipelineService — 销售漏斗")
class PipelineServiceTest {

    private PipelineStageMapper stageMapper;
    private OpportunityMapper opportunityMapper;
    private OpportunityStageLogMapper stageLogMapper;
    private PipelineService service;

    @BeforeEach
    void setUp() {
        stageMapper = mock(PipelineStageMapper.class);
        opportunityMapper = mock(OpportunityMapper.class);
        stageLogMapper = mock(OpportunityStageLogMapper.class);
        service = new PipelineService(stageMapper, opportunityMapper, stageLogMapper);
    }

    private PipelineStage stage(long id, String code) {
        PipelineStage s = PipelineStage.builder()
                .name(code).code(code).orderNo(1).winRate(50).active(1).isTerminal(0).build();
        s.setId(id);
        return s;
    }

    @Test
    @DisplayName("创建商机：未指定阶段时取 LEAD 默认阶段")
    void create_defaultsToLead() {
        when(stageMapper.selectOne(any())).thenReturn(stage(10L, PipelineStage.CODE_LEAD));

        Long id = service.createOpportunity(OpportunityCreateDTO.builder()
                .customerId(100L).projectId(200L).ownerId(300L).build());

        ArgumentCaptor<Opportunity> opp = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityMapper).insert(opp.capture());
        assertThat(opp.getValue().getStageId()).isEqualTo(10L);
        assertThat(opp.getValue().getCustomerId()).isEqualTo(100L);
        assertThat(opp.getValue().getLastStageChangedAt()).isNotNull();

        ArgumentCaptor<OpportunityStageLog> log = ArgumentCaptor.forClass(OpportunityStageLog.class);
        verify(stageLogMapper).insert(log.capture());
        assertThat(log.getValue().getFromStageId()).isNull();
        assertThat(log.getValue().getToStageId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("创建商机：customerId 缺失抛 IllegalArgumentException")
    void create_missingCustomer() {
        assertThatThrownBy(() -> service.createOpportunity(
                OpportunityCreateDTO.builder().projectId(200L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("客户");
    }

    @Test
    @DisplayName("创建商机：projectId 缺失抛 IllegalArgumentException")
    void create_missingProject() {
        assertThatThrownBy(() -> service.createOpportunity(
                OpportunityCreateDTO.builder().customerId(100L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("项目");
    }

    @Test
    @DisplayName("创建商机：默认 LEAD 阶段未初始化时抛 IllegalStateException")
    void create_noLeadStage() {
        when(stageMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createOpportunity(
                OpportunityCreateDTO.builder().customerId(100L).projectId(200L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEAD");
    }

    @Test
    @DisplayName("移动阶段：正常流转 LEAD→INTENT，写日志 + 更新 lastStageChangedAt")
    void moveStage_normal() {
        Opportunity opp = Opportunity.builder().stageId(10L).build();
        opp.setId(500L);
        when(opportunityMapper.selectById(500L)).thenReturn(opp);
        when(stageMapper.selectById(20L)).thenReturn(stage(20L, PipelineStage.CODE_INTENT));

        service.moveStage(OpportunityMoveStageDTO.builder()
                .opportunityId(500L).toStageId(20L).operatorId(99L).remark("电话沟通").build());

        assertThat(opp.getStageId()).isEqualTo(20L);
        assertThat(opp.getLastStageChangedAt()).isNotNull();

        ArgumentCaptor<OpportunityStageLog> log = ArgumentCaptor.forClass(OpportunityStageLog.class);
        verify(stageLogMapper).insert(log.capture());
        assertThat(log.getValue().getFromStageId()).isEqualTo(10L);
        assertThat(log.getValue().getToStageId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("移动阶段：同阶段 no-op，不更新不写日志")
    void moveStage_sameStage_noop() {
        Opportunity opp = Opportunity.builder().stageId(10L).build();
        opp.setId(500L);
        when(opportunityMapper.selectById(500L)).thenReturn(opp);

        service.moveStage(OpportunityMoveStageDTO.builder()
                .opportunityId(500L).toStageId(10L).build());

        verify(opportunityMapper, never()).updateById(any());
        verify(stageLogMapper, never()).insert(any());
    }

    @Test
    @DisplayName("移动阶段：流失到 LOST 必须填备注")
    void moveStage_lostRequiresRemark() {
        Opportunity opp = Opportunity.builder().stageId(10L).build();
        opp.setId(500L);
        when(opportunityMapper.selectById(500L)).thenReturn(opp);

        PipelineStage lost = stage(60L, PipelineStage.CODE_LOST);
        lost.setIsTerminal(1);
        when(stageMapper.selectById(60L)).thenReturn(lost);

        assertThatThrownBy(() -> service.moveStage(OpportunityMoveStageDTO.builder()
                .opportunityId(500L).toStageId(60L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流失原因");
    }

    @Test
    @DisplayName("移动阶段：流失到 LOST 含备注 → 写入 lostReason")
    void moveStage_lostWithRemark() {
        Opportunity opp = Opportunity.builder().stageId(10L).build();
        opp.setId(500L);
        when(opportunityMapper.selectById(500L)).thenReturn(opp);

        PipelineStage lost = stage(60L, PipelineStage.CODE_LOST);
        lost.setIsTerminal(1);
        when(stageMapper.selectById(60L)).thenReturn(lost);

        service.moveStage(OpportunityMoveStageDTO.builder()
                .opportunityId(500L).toStageId(60L).remark("客户预算不足").build());

        assertThat(opp.getLostReason()).isEqualTo("客户预算不足");
        assertThat(opp.getStageId()).isEqualTo(60L);
    }

    @Test
    @DisplayName("移动阶段：商机不存在抛异常")
    void moveStage_notFound() {
        when(opportunityMapper.selectById(any())).thenReturn(null);

        assertThatThrownBy(() -> service.moveStage(OpportunityMoveStageDTO.builder()
                .opportunityId(999L).toStageId(20L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("移动阶段：目标阶段不存在/已禁用抛异常")
    void moveStage_targetInvalid() {
        Opportunity opp = Opportunity.builder().stageId(10L).build();
        opp.setId(500L);
        when(opportunityMapper.selectById(500L)).thenReturn(opp);
        when(stageMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.moveStage(OpportunityMoveStageDTO.builder()
                .opportunityId(500L).toStageId(99L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标阶段");
    }
}
