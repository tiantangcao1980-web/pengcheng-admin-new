package com.pengcheng.crm.lead;

import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.lead.dto.LeadAssignDTO;
import com.pengcheng.crm.lead.entity.CrmLead;
import com.pengcheng.crm.lead.entity.CrmLeadAssignment;
import com.pengcheng.crm.lead.mapper.CrmLeadAssignmentMapper;
import com.pengcheng.crm.lead.mapper.CrmLeadMapper;
import com.pengcheng.crm.lead.service.LeadAssignmentRule;
import com.pengcheng.crm.lead.service.LeadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * LeadAssignmentE2EIntegrationTest — 聚焦分配规则引擎与 LeadService 的集成。
 * <p>
 * 覆盖：ROUND_ROBIN 轮询、RANDOM（load_balance 最小负载）、MANUAL 手动、
 * 候选池为空时的 null 返回行为，以及批量多规则场景。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeadAssignment 端到端集成测试：分配规则引擎 + LeadService")
class LeadAssignmentE2EIntegrationTest {

    @Mock
    private CrmLeadMapper leadMapper;

    @Mock
    private CrmLeadAssignmentMapper assignmentMapper;

    @InjectMocks
    private LeadService leadService;

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 1：ROUND_ROBIN — 连续 3 条线索轮询分配给 3 个 sales 用户
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("1. ROUND_ROBIN：3 条线索依次分配给 [101,102,103]")
    void roundRobin_threLeads_distributedToThreeSales() {
        // 准备 3 条线索
        CrmLead lead1 = buildLead(1L, "manual");
        CrmLead lead2 = buildLead(2L, "manual");
        CrmLead lead3 = buildLead(3L, "manual");
        when(leadMapper.selectById(1L)).thenReturn(lead1);
        when(leadMapper.selectById(2L)).thenReturn(lead2);
        when(leadMapper.selectById(3L)).thenReturn(lead3);
        when(leadMapper.updateById(any())).thenReturn(1);
        when(assignmentMapper.insert(any())).thenReturn(1);

        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Arrays.asList(1L, 2L, 3L));
        dto.setRuleType("round_robin");
        dto.setCandidateUserIds(Arrays.asList(101L, 102L, 103L));

        // 使用共享 currentLoad（含轮询游标状态）
        Map<Long, Integer> sharedLoad = new HashMap<>();
        int affected = leadService.assign(dto, 0L, sharedLoad);

        assertEquals(3, affected, "3 条线索全部应分配成功");

        // 捕获所有 assignment 记录，验证轮询顺序
        ArgumentCaptor<CrmLeadAssignment> captor = ArgumentCaptor.forClass(CrmLeadAssignment.class);
        verify(assignmentMapper, times(3)).insert(captor.capture());
        List<CrmLeadAssignment> logs = captor.getAllValues();

        assertEquals(101L, logs.get(0).getToUserId(), "第1条 → 101");
        assertEquals(102L, logs.get(1).getToUserId(), "第2条 → 102");
        assertEquals(103L, logs.get(2).getToUserId(), "第3条 → 103");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 2：ROUND_ROBIN 第 4 条线索应回到第 1 个用户（wrap-around）
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("2. ROUND_ROBIN wrap-around：第 4 条分配回 101")
    void roundRobin_fourthLead_wrapsToFirst() {
        // 直接测试纯函数（LeadService 内部调用 LeadAssignmentRule.pick）
        Map<Long, Integer> state = new HashMap<>();
        List<Long> candidates = Arrays.asList(101L, 102L, 103L);

        Long a = LeadAssignmentRule.pick("round_robin", null, candidates, state, null, null);
        Long b = LeadAssignmentRule.pick("round_robin", null, candidates, state, null, null);
        Long c = LeadAssignmentRule.pick("round_robin", null, candidates, state, null, null);
        Long d = LeadAssignmentRule.pick("round_robin", null, candidates, state, null, null);

        assertEquals(101L, a);
        assertEquals(102L, b);
        assertEquals(103L, c);
        assertEquals(101L, d, "第 4 次应 wrap-around 回 101");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 3：LOAD_BALANCE（业务文档称 "RANDOM" 语义：选负载最小候选）→ 1 条分配给最低负载用户
    //
    // 注：D3 LeadAssignmentRule 未实现 "random" 关键字，以 "load_balance" 实现
    //     "从候选池选负载最小的用户"语义。业务需求文档若要求 "random" 关键字，
    //     TODO: 需在 LeadAssignmentRule 的 switch 中增加 "random" -> candidates random pick 分支。
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("3. LOAD_BALANCE：1 条线索分配给当前负载最小的候选用户")
    void loadBalance_oneLeadAssignedToLeastLoadedUser() {
        CrmLead lead = buildLead(10L, "form");
        when(leadMapper.selectById(10L)).thenReturn(lead);
        when(leadMapper.updateById(any())).thenReturn(1);
        when(assignmentMapper.insert(any())).thenReturn(1);

        // 候选人负载：201→5，202→1，203→9 → 应选 202
        Map<Long, Integer> currentLoad = new HashMap<>();
        currentLoad.put(201L, 5);
        currentLoad.put(202L, 1);
        currentLoad.put(203L, 9);

        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Collections.singletonList(10L));
        dto.setRuleType("load_balance");
        dto.setCandidateUserIds(Arrays.asList(201L, 202L, 203L));

        int affected = leadService.assign(dto, 0L, currentLoad);
        assertEquals(1, affected);

        ArgumentCaptor<CrmLeadAssignment> captor = ArgumentCaptor.forClass(CrmLeadAssignment.class);
        verify(assignmentMapper).insert(captor.capture());
        assertEquals(202L, captor.getValue().getToUserId(), "应分配给负载最低的 202");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 4：MANUAL 规则 — 直接分配给 targetUserId，assignment 记录 ruleType=manual
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("4. MANUAL：直接分配给指定 targetUserId，不经过候选池轮询")
    void manual_assignsToExactTarget() {
        CrmLead lead = buildLead(20L, "qrcode");
        when(leadMapper.selectById(20L)).thenReturn(lead);
        when(leadMapper.updateById(any())).thenReturn(1);
        when(assignmentMapper.insert(any())).thenReturn(1);

        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Collections.singletonList(20L));
        dto.setRuleType("manual");
        dto.setTargetUserId(300L);
        dto.setNote("手动指派给销售 300");

        int affected = leadService.assign(dto, 99L /* currentUserId */, new HashMap<>());
        assertEquals(1, affected);

        ArgumentCaptor<CrmLeadAssignment> captor = ArgumentCaptor.forClass(CrmLeadAssignment.class);
        verify(assignmentMapper).insert(captor.capture());
        CrmLeadAssignment log = captor.getValue();
        assertEquals(300L, log.getToUserId());
        assertEquals("manual", log.getRuleType());
        assertEquals(99L, log.getAssignedBy());
        assertEquals("手动指派给销售 300", log.getNote());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 5：MANUAL 规则且 targetUserId = null → pick 返回 null → 线索被跳过，affected=0
    //
    // 这是一种"待管理员手动"的中间状态：规则类型是 manual 但没有指定目标用户。
    // LeadService 在 pick 返回 null 时会 continue（不分配）。
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("5. MANUAL + targetUserId=null → pick 返回 null，线索不被分配(affected=0)")
    void manual_withNullTarget_skipsLead() {
        CrmLead lead = buildLead(30L, "import");
        when(leadMapper.selectById(30L)).thenReturn(lead);

        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Collections.singletonList(30L));
        dto.setRuleType("manual");
        dto.setTargetUserId(null); // 未指定目标

        int affected = leadService.assign(dto, 0L, new HashMap<>());

        assertEquals(0, affected, "targetUserId=null 时 manual 应返回 null，线索不被分配");
        verify(assignmentMapper, never()).insert(any());
        verify(leadMapper, never()).updateById(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 6：候选池为空时 round_robin 返回 null → 兜底（affected=0，不写 assignment）
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("6. 候选池为空 + round_robin → pick 返回 null，不分配，affected=0")
    void roundRobin_emptyCandidates_fallbackToZeroAffected() {
        CrmLead lead = buildLead(40L, "api");
        when(leadMapper.selectById(40L)).thenReturn(lead);

        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Collections.singletonList(40L));
        dto.setRuleType("round_robin");
        dto.setCandidateUserIds(Collections.emptyList()); // 空候选池

        int affected = leadService.assign(dto, 0L, new HashMap<>());

        assertEquals(0, affected, "候选池为空时应 fallback 为 0（不分配）");
        verify(assignmentMapper, never()).insert(any());

        // TODO: 当候选池为空时，建议业务上 fallback to admin（管理员 userId）而非静默跳过。
        //       当前 LeadService 直接 continue，可能导致线索永久滞留"待分配"状态，
        //       需要在 assign 方法中增加 fallbackAdminId 兜底逻辑。
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 7：RULE 规则 + source 命中 sourceMapping → 分配给映射用户
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("7. RULE 规则：source 命中映射 → 分配给映射用户而非轮询候选")
    void ruleMode_sourceHitsMapping_assignsMappedUser() {
        // LeadAssignmentRule 直接测试（service 层 assign 方法中 sourceMapping 参数传 null）
        // 此用例直接测试 pick 函数的 rule 模式
        Map<String, Long> mapping = new HashMap<>();
        mapping.put("qrcode", 777L);
        mapping.put("form", 888L);

        Long picked = LeadAssignmentRule.pick("rule", null,
                Arrays.asList(101L, 102L), new HashMap<>(), "qrcode", mapping);

        assertEquals(777L, picked, "source=qrcode 应命中映射 userId=777");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 8：LeadAssignDTO.leadIds 为空 → 抛出 BusinessException
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("8. leadIds 为空 → 抛出 BusinessException")
    void assign_emptyLeadIds_throwsBusinessException() {
        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Collections.emptyList());

        assertThrows(BusinessException.class,
                () -> leadService.assign(dto, 0L, new HashMap<>()));
    }

    // ─── 辅助方法 ────────────────────────────────────────────────────────────

    private CrmLead buildLead(Long id, String source) {
        CrmLead lead = new CrmLead();
        try {
            java.lang.reflect.Field f = com.pengcheng.common.entity.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(lead, id);
        } catch (Exception e) {
            throw new RuntimeException("无法设置 id 字段", e);
        }
        lead.setName("线索-" + id);
        lead.setSource(source);
        lead.setStatus(1);
        return lead;
    }
}
