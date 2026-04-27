package com.pengcheng.crm.lead;

import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.lead.controller.LeadController;
import com.pengcheng.crm.lead.controller.LeadFormController;
import com.pengcheng.crm.lead.dto.LeadAssignDTO;
import com.pengcheng.crm.lead.dto.LeadConvertDTO;
import com.pengcheng.crm.lead.dto.LeadCreateDTO;
import com.pengcheng.crm.lead.dto.PublicLeadSubmitDTO;
import com.pengcheng.crm.lead.entity.CrmLead;
import com.pengcheng.crm.lead.entity.CrmLeadAssignment;
import com.pengcheng.crm.lead.entity.CrmLeadForm;
import com.pengcheng.crm.lead.mapper.CrmLeadAssignmentMapper;
import com.pengcheng.crm.lead.mapper.CrmLeadFormMapper;
import com.pengcheng.crm.lead.mapper.CrmLeadMapper;
import com.pengcheng.crm.lead.service.LeadFormService;
import com.pengcheng.crm.lead.service.LeadService;
import com.pengcheng.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * LeadE2EIntegrationTest — 验证"公开表单提交 → 线索创建 → 分配 → 转客户"完整链路。
 * 不启动 Spring 上下文，仅通过 Mockito 模拟 mapper 依赖。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Lead 端到端集成测试：表单提交 → 分配 → 转客户")
class LeadE2EIntegrationTest {

    // ─── LeadService 所需 Mocks ──────────────────────────────────────────────
    @Mock
    private CrmLeadMapper leadMapper;

    @Mock
    private CrmLeadAssignmentMapper assignmentMapper;

    @InjectMocks
    private LeadService leadService;

    // ─── LeadFormService 所需 Mocks ─────────────────────────────────────────
    @Mock
    private CrmLeadFormMapper formMapper;

    // LeadFormService 依赖 LeadService（通过 @Autowired 注入），
    // 此处使用真实 LeadService 实例（已通过 leadMapper mock 隔离数据库）。
    private LeadFormService leadFormService;

    // ─── Controllers ────────────────────────────────────────────────────────
    private LeadController leadController;
    private LeadFormController leadFormController;

    @BeforeEach
    void setUp() throws Exception {
        // 手动组装：将已 mock 好的 leadService 注入 LeadFormService
        leadFormService = new LeadFormService();
        setField(leadFormService, "formMapper", formMapper);
        setField(leadFormService, "leadService", leadService);

        // 组装 Controller
        leadController = new LeadController();
        setField(leadController, "leadService", leadService);

        leadFormController = new LeadFormController();
        setField(leadFormController, "formService", leadFormService);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 1：公开表单提交 → status=NEW(1) → leadMapper.insert 被调一次
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("1. 公开表单提交成功 → 创建 CrmLead(status=1) 且 mapper.insert 被调一次")
    void publicSubmit_createsLead_withStatusNew() {
        // given: 一个已启用的表单，defaultOwnerId 为空（待分配）
        CrmLeadForm form = buildEnabledForm("FORM_001", null);
        when(formMapper.selectOne(any())).thenReturn(form);
        when(formMapper.updateById(any())).thenReturn(1);
        when(leadMapper.insert(any(CrmLead.class))).thenReturn(1);

        PublicLeadSubmitDTO dto = buildPublicSubmit("FORM_001", "张三", "13800001111", null);

        // when
        Result<CrmLead> result = leadFormController.submit(dto);

        // then
        assertNotNull(result);
        assertEquals(200, result.getCode());
        CrmLead created = result.getData();
        assertNotNull(created);
        assertEquals("张三", created.getName());
        // 无 defaultOwnerId → status 应为 1（待分配）
        assertEquals(1, created.getStatus(), "未指定 owner 时 status 应为 1(待分配)");
        assertNull(created.getOwnerId(), "无 defaultOwnerId 时 ownerId 应为 null");

        // verify mapper 被调用
        verify(leadMapper, times(1)).insert(any(CrmLead.class));
        verify(formMapper, times(1)).updateById(any()); // submitCount +1
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 2：表单 defaultOwnerId 非空 → status=2(已分配)
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("2. 表单含 defaultOwnerId → 创建 CrmLead(status=2, ownerId 已赋值)")
    void publicSubmit_withDefaultOwner_setsStatusAssigned() {
        CrmLeadForm form = buildEnabledForm("FORM_002", 88L);
        when(formMapper.selectOne(any())).thenReturn(form);
        when(formMapper.updateById(any())).thenReturn(1);
        when(leadMapper.insert(any(CrmLead.class))).thenReturn(1);

        PublicLeadSubmitDTO dto = buildPublicSubmit("FORM_002", "李四", "13900002222", null);

        CrmLead created = leadFormController.submit(dto).getData();

        assertEquals(2, created.getStatus(), "defaultOwnerId 非空时 status 应为 2(已分配)");
        assertEquals(88L, created.getOwnerId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 3：管理员分配 → lead.ownerId 更新 + assignment 记录写入（调用顺序验证）
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("3. assign(LeadAssignDTO) → 更新 lead.ownerId 并写入 assignment 记录")
    void assign_updatesOwnerAndWritesAssignmentLog() {
        // given
        CrmLead existingLead = buildLead(100L, null, 1);
        when(leadMapper.selectById(100L)).thenReturn(existingLead);
        when(leadMapper.updateById(any(CrmLead.class))).thenReturn(1);
        when(assignmentMapper.insert(any(CrmLeadAssignment.class))).thenReturn(1);

        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Arrays.asList(100L));
        dto.setRuleType("manual");
        dto.setTargetUserId(55L);
        dto.setNote("测试分配");

        // when
        Result<Integer> result = leadController.assign(dto);

        // then
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData(), "应成功分配 1 条线索");

        // 验证调用顺序：先查 lead → 再写 assignment log → 再 updateById
        InOrder inOrder = inOrder(leadMapper, assignmentMapper);
        inOrder.verify(leadMapper).selectById(100L);
        inOrder.verify(assignmentMapper).insert(any(CrmLeadAssignment.class));
        inOrder.verify(leadMapper).updateById(any(CrmLead.class));

        // 验证 assignment 记录内容
        ArgumentCaptor<CrmLeadAssignment> logCaptor = ArgumentCaptor.forClass(CrmLeadAssignment.class);
        verify(assignmentMapper).insert(logCaptor.capture());
        CrmLeadAssignment log = logCaptor.getValue();
        assertEquals(100L, log.getLeadId());
        assertEquals(55L, log.getToUserId());
        assertEquals("manual", log.getRuleType());

        // 验证 lead 被更新
        ArgumentCaptor<CrmLead> leadCaptor = ArgumentCaptor.forClass(CrmLead.class);
        verify(leadMapper).updateById(leadCaptor.capture());
        CrmLead updated = leadCaptor.getValue();
        assertEquals(55L, updated.getOwnerId());
        assertEquals(2, updated.getStatus());
        assertNotNull(updated.getAssignTime());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 4：转客户（customerId 已有）→ status=4 + customerId 写入
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("4. convert(LeadConvertDTO, customerId 非空) → status=4 + convertTime 写入")
    void convert_withCustomerId_setsStatusConverted() {
        CrmLead lead = buildLead(200L, 55L, 3 /* 跟进中 */);
        when(leadMapper.selectById(200L)).thenReturn(lead);
        when(leadMapper.updateById(any(CrmLead.class))).thenReturn(1);

        LeadConvertDTO dto = new LeadConvertDTO();
        dto.setLeadId(200L);
        dto.setCustomerId(999L);
        dto.setRemark("转化成功");

        Result<CrmLead> result = leadController.convert(dto);

        assertEquals(200, result.getCode());
        CrmLead converted = result.getData();
        assertEquals(4, converted.getStatus(), "转客户后 status 应为 4");
        assertEquals(999L, converted.getCustomerId());
        assertNotNull(converted.getConvertTime());
        assertEquals("转化成功", converted.getRemark());

        verify(leadMapper, times(1)).updateById(any(CrmLead.class));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 5：convert 时 customerId 为 null → 抛出 BusinessException
    //
    // TODO: D3 production 代码 convertToCustomer 强制要求 customerId 非空，
    //       但 LeadConvertDTO.customerName 字段暗示"创建新客户"场景，
    //       实际无法单独靠 customerName 创建客户（service 层直接抛异常）。
    //       这是一处设计矛盾：DTO 定义了"创建新客户"语义但 Service 不支持。
    //       建议 Facade 层在调用 convertToCustomer 前先创建 customer 并回填 customerId。
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("5. convert(LeadConvertDTO, customerId=null) → 抛出 BusinessException")
    void convert_withoutCustomerId_throwsBusinessException() {
        CrmLead lead = buildLead(300L, 55L, 2);
        when(leadMapper.selectById(300L)).thenReturn(lead);

        LeadConvertDTO dto = new LeadConvertDTO();
        dto.setLeadId(300L);
        dto.setCustomerId(null); // 不提供 customerId

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leadService.convertToCustomer(dto));
        assertTrue(ex.getMessage().contains("customerId"),
                "异常信息应提示 customerId 缺失原因");

        // 线索状态不应被改变
        verify(leadMapper, never()).updateById(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 6：对已转客户的线索重复 convert → 抛出 BusinessException
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("6. 对 status=4 线索重复 convert → 抛出 BusinessException(线索已转客户)")
    void convert_alreadyConverted_throwsBusinessException() {
        CrmLead lead = buildLead(400L, 55L, 4 /* 已转客户 */);
        when(leadMapper.selectById(400L)).thenReturn(lead);

        LeadConvertDTO dto = new LeadConvertDTO();
        dto.setLeadId(400L);
        dto.setCustomerId(888L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leadService.convertToCustomer(dto));
        assertTrue(ex.getMessage().contains("已转客户"));
        verify(leadMapper, never()).updateById(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 7：assign leadId 不存在 → 跳过，返回 affected=0
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("7. assign 不存在的 leadId → 跳过该条，返回 affected=0")
    void assign_leadNotFound_skipsAndReturnsZero() {
        when(leadMapper.selectById(anyLong())).thenReturn(null);

        LeadAssignDTO dto = new LeadAssignDTO();
        dto.setLeadIds(Arrays.asList(9999L));
        dto.setRuleType("manual");
        dto.setTargetUserId(10L);

        int affected = leadService.assign(dto, 0L, new HashMap<>());

        assertEquals(0, affected);
        verify(assignmentMapper, never()).insert(any());
        verify(leadMapper, never()).updateById(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 8：assign 参数为 null → 抛出 BusinessException
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("8. assign(null) → 抛出 BusinessException(参数校验)")
    void assign_nullDto_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> leadService.assign(null, 0L, new HashMap<>()));
        verify(leadMapper, never()).selectById(any());
    }

    // ─── 辅助方法 ────────────────────────────────────────────────────────────

    private CrmLeadForm buildEnabledForm(String formCode, Long defaultOwnerId) {
        CrmLeadForm form = new CrmLeadForm();
        form.setFormCode(formCode);
        form.setTitle("测试表单");
        form.setEnabled(1);
        form.setSubmitCount(0);
        form.setDefaultSource("form");
        form.setDefaultOwnerId(defaultOwnerId);
        return form;
    }

    private CrmLead buildLead(Long id, Long ownerId, int status) {
        CrmLead lead = new CrmLead();
        // BaseEntity 的 id 字段，通过反射设置（避免依赖 Spring 上下文）
        try {
            java.lang.reflect.Field f = com.pengcheng.common.entity.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(lead, id);
        } catch (Exception e) {
            throw new RuntimeException("无法设置 id 字段", e);
        }
        lead.setOwnerId(ownerId);
        lead.setStatus(status);
        lead.setName("测试线索-" + id);
        lead.setSource("manual");
        return lead;
    }

    private PublicLeadSubmitDTO buildPublicSubmit(String formCode, String name, String phone, String email) {
        PublicLeadSubmitDTO dto = new PublicLeadSubmitDTO();
        dto.setFormCode(formCode);
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("phone", phone);
        if (email != null) fields.put("email", email);
        dto.setFields(fields);
        return dto;
    }

    /** 通过反射注入字段（替代 Spring @Autowired） */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("字段 " + fieldName + " 不存在于 " + target.getClass().getName());
    }
}
