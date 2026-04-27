package com.pengcheng.crm.lead;

import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.lead.controller.LeadFormController;
import com.pengcheng.crm.lead.dto.PublicLeadSubmitDTO;
import com.pengcheng.crm.lead.entity.CrmLead;
import com.pengcheng.crm.lead.entity.CrmLeadForm;
import com.pengcheng.crm.lead.mapper.CrmLeadFormMapper;
import com.pengcheng.crm.lead.mapper.CrmLeadMapper;
import com.pengcheng.crm.lead.mapper.CrmLeadAssignmentMapper;
import com.pengcheng.crm.lead.service.LeadFormService;
import com.pengcheng.crm.lead.service.LeadService;
import com.pengcheng.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LeadFormPublicSubmitE2ETest — 公开表单提交的边界与兜底场景。
 * <p>
 * 覆盖：
 * 1. 正常提交（最小字段：仅 name）
 * 2. fields 为 null → 400 BusinessException
 * 3. name 字段缺失 → 400 BusinessException
 * 4. 表单已停用 → BusinessException
 * 5. formCode 不存在 → BusinessException
 * 6. 同一手机号重复提交（无限流实现时：验证 formSubmitCount 累加行为）
 * 7. 未登录用户（无权限上下文）可正常提交（公开端点无需鉴权）
 *
 * 注：LeadFormService 当前未实现频率限流，用例 6 记录 TODO 并验证现有行为。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeadFormPublicSubmit 端到端测试：公开表单提交边界与安全")
class LeadFormPublicSubmitE2ETest {

    @Mock
    private CrmLeadFormMapper formMapper;

    @Mock
    private CrmLeadMapper leadMapper;

    @Mock
    private CrmLeadAssignmentMapper assignmentMapper;

    // 真实 LeadService（mapper 已 mock）
    private LeadService leadService;

    // 真实 LeadFormService（formMapper + leadService 已 mock/真实）
    private LeadFormService leadFormService;

    // 真实 LeadFormController（formService 已组装）
    private LeadFormController leadFormController;

    @BeforeEach
    void setUp() throws Exception {
        leadService = new LeadService();
        setField(leadService, "leadMapper", leadMapper);
        setField(leadService, "assignmentMapper", assignmentMapper);

        leadFormService = new LeadFormService();
        setField(leadFormService, "formMapper", formMapper);
        setField(leadFormService, "leadService", leadService);

        leadFormController = new LeadFormController();
        setField(leadFormController, "formService", leadFormService);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 1：正常提交最小字段（仅 name） → 创建线索成功
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("1. 最小字段提交（仅 name）→ 成功创建线索，返回 200")
    void submit_minimalFields_successWithNameOnly() {
        CrmLeadForm form = buildForm("FORM_MIN", 1, null);
        when(formMapper.selectOne(any())).thenReturn(form);
        when(formMapper.updateById(any())).thenReturn(1);
        when(leadMapper.insert(any())).thenReturn(1);

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "王五");
        PublicLeadSubmitDTO dto = buildDto("FORM_MIN", fields);

        Result<CrmLead> result = leadFormController.submit(dto);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("王五", result.getData().getName());
        // 手机号未传 → phoneMasked 为 null
        assertNull(result.getData().getPhone());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 2：fields 为 null → 抛出 BusinessException(400)
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("2. fields=null → 抛出 BusinessException(fields 不能为空)")
    void submit_nullFields_throwsBusinessException() {
        CrmLeadForm form = buildForm("FORM_NULL", 1, null);
        when(formMapper.selectOne(any())).thenReturn(form);

        PublicLeadSubmitDTO dto = new PublicLeadSubmitDTO();
        dto.setFormCode("FORM_NULL");
        dto.setFields(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leadFormController.submit(dto));
        assertTrue(ex.getMessage().contains("fields"), "异常信息应提示 fields 不能为空");
        verify(leadMapper, never()).insert(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 3：name 字段缺失（fields 存在但 name=null）→ 抛出 BusinessException(400)
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("3. fields 中 name 缺失 → 抛出 BusinessException(name 字段必填)")
    void submit_missingNameField_throwsBusinessException() {
        CrmLeadForm form = buildForm("FORM_NO_NAME", 1, null);
        when(formMapper.selectOne(any())).thenReturn(form);

        Map<String, Object> fields = new HashMap<>();
        fields.put("phone", "13800001234");
        // 故意不传 name
        PublicLeadSubmitDTO dto = buildDto("FORM_NO_NAME", fields);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leadFormController.submit(dto));
        assertTrue(ex.getMessage().contains("name"), "异常信息应提示 name 字段必填");
        verify(leadMapper, never()).insert(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 4：表单已停用（enabled=0）→ 抛出 BusinessException
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("4. 表单已停用(enabled=0) → 抛出 BusinessException(表单已停用)")
    void submit_disabledForm_throwsBusinessException() {
        CrmLeadForm form = buildForm("FORM_DISABLED", 0, null);
        when(formMapper.selectOne(any())).thenReturn(form);

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "测试用户");
        PublicLeadSubmitDTO dto = buildDto("FORM_DISABLED", fields);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leadFormController.submit(dto));
        assertTrue(ex.getMessage().contains("停用"), "异常信息应提示表单已停用");
        verify(leadMapper, never()).insert(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 5：formCode 不存在 → 抛出 BusinessException(表单不存在)
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("5. formCode 不存在 → 抛出 BusinessException(表单不存在)")
    void submit_unknownFormCode_throwsBusinessException() {
        when(formMapper.selectOne(any())).thenReturn(null); // 查不到表单

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "测试用户");
        PublicLeadSubmitDTO dto = buildDto("NONEXISTENT_CODE", fields);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leadFormController.submit(dto));
        assertTrue(ex.getMessage().contains("表单"), "异常信息应提示表单不存在");
        verify(leadMapper, never()).insert(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 6：同一手机号重复提交（模拟 1 分钟内 2 次）
    //
    // TODO: LeadFormService 当前未实现同手机号频率限流（如 Redis 令牌桶/滑动窗口）。
    //       当前行为：2 次提交均成功，创建 2 条 CrmLead 记录，leadMapper.insert 被调 2 次。
    //       建议在 LeadFormService.submit 中增加如下防抖逻辑：
    //         if (leadMapper.selectCount(phone + 60s) > 0) throw new BusinessException(429, "提交过于频繁")
    //       届时本用例应改为 assertThrows(BusinessException.class, second_submit)。
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("6. 同手机号重复提交(当前无限流) → 两次均成功，insert 被调 2 次")
    void submit_duplicatePhone_noRateLimitCurrently_bothSucceed() {
        CrmLeadForm form = buildForm("FORM_DUP", 1, null);
        // 每次 selectOne 都返回同一个 form 对象（模拟两次独立提交）
        when(formMapper.selectOne(any())).thenReturn(form);
        when(formMapper.updateById(any())).thenReturn(1);
        when(leadMapper.insert(any())).thenReturn(1);

        Map<String, Object> fields1 = new HashMap<>();
        fields1.put("name", "赵六");
        fields1.put("phone", "13911112222");

        Map<String, Object> fields2 = new HashMap<>();
        fields2.put("name", "赵六");
        fields2.put("phone", "13911112222"); // 同手机号

        PublicLeadSubmitDTO first  = buildDto("FORM_DUP", fields1);
        PublicLeadSubmitDTO second = buildDto("FORM_DUP", fields2);

        // 第一次提交
        Result<CrmLead> r1 = leadFormController.submit(first);
        assertEquals(200, r1.getCode());

        // 第二次提交（当前无限流，应同样成功）
        Result<CrmLead> r2 = leadFormController.submit(second);
        assertEquals(200, r2.getCode());

        // 当前行为：两次均调用 insert
        verify(leadMapper, times(2)).insert(any());
        // submitCount 累加两次
        verify(formMapper, times(2)).updateById(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 7：未登录用户提交（公开端点，无 Spring Security / Sa-Token 上下文）
    //
    // LeadFormController.submit 上没有 @SaCheckPermission，
    // 本测试直接调用 controller 方法（无 Spring 上下文），模拟无权限上下文提交。
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("7. 无鉴权上下文（未登录用户）提交公开表单 → 正常创建线索")
    void submit_noAuthContext_publicEndpointAllowed() {
        CrmLeadForm form = buildForm("FORM_PUBLIC", 1, null);
        when(formMapper.selectOne(any())).thenReturn(form);
        when(formMapper.updateById(any())).thenReturn(1);
        when(leadMapper.insert(any())).thenReturn(1);

        // 模拟无 Cookie / 无 Token 场景：直接构造 DTO（无需 request 上下文）
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "匿名访客");
        fields.put("phone", "13700007777");
        PublicLeadSubmitDTO dto = buildDto("FORM_PUBLIC", fields);

        // 期望：公开端点不需要鉴权，应正常返回 200
        assertDoesNotThrow(() -> {
            Result<CrmLead> result = leadFormController.submit(dto);
            assertEquals(200, result.getCode());
            assertEquals("匿名访客", result.getData().getName());
        });

        verify(leadMapper, times(1)).insert(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 8：email 字段为空字符串 → 允许提交（phone/email/wechat 均非必填）
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("8. email 为空字符串 → 允许提交，email 字段存储空字符串")
    void submit_emptyEmailField_successfullyCreatesLead() {
        CrmLeadForm form = buildForm("FORM_EMPTY_EMAIL", 1, null);
        when(formMapper.selectOne(any())).thenReturn(form);
        when(formMapper.updateById(any())).thenReturn(1);
        when(leadMapper.insert(any())).thenReturn(1);

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "空 email 用户");
        fields.put("email", ""); // 空字符串

        PublicLeadSubmitDTO dto = buildDto("FORM_EMPTY_EMAIL", fields);
        Result<CrmLead> result = leadFormController.submit(dto);

        assertEquals(200, result.getCode());
        // LeadFormService 将 "" 通过 stringOf 转换，email 为空字符串
        // 注：LeadCreateDTO 未对 email 做非空校验，允许空值，这是合理的
        verify(leadMapper, times(1)).insert(any());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 用例 9：submitCount 随每次成功提交递增（form 表单计数器验证）
    // ═══════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("9. 每次成功提交 → form.submitCount 自增 1")
    void submit_success_incrementsSubmitCount() {
        CrmLeadForm form = buildForm("FORM_COUNT", 1, null);
        form.setSubmitCount(5); // 初始已有 5 次

        when(formMapper.selectOne(any())).thenReturn(form);
        when(formMapper.updateById(any())).thenReturn(1);
        when(leadMapper.insert(any())).thenReturn(1);

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "计数测试用户");
        PublicLeadSubmitDTO dto = buildDto("FORM_COUNT", fields);

        leadFormController.submit(dto);

        // 验证 updateById 被调用，且 submitCount 已变为 6
        org.mockito.ArgumentCaptor<CrmLeadForm> formCaptor =
                org.mockito.ArgumentCaptor.forClass(CrmLeadForm.class);
        verify(formMapper).updateById(formCaptor.capture());
        assertEquals(6, formCaptor.getValue().getSubmitCount(), "submitCount 应从 5 增加到 6");
    }

    // ─── 辅助方法 ────────────────────────────────────────────────────────────

    private CrmLeadForm buildForm(String code, int enabled, Long defaultOwnerId) {
        CrmLeadForm form = new CrmLeadForm();
        form.setFormCode(code);
        form.setTitle("测试表单-" + code);
        form.setEnabled(enabled);
        form.setSubmitCount(0);
        form.setDefaultSource("form");
        form.setDefaultOwnerId(defaultOwnerId);
        return form;
    }

    private PublicLeadSubmitDTO buildDto(String formCode, Map<String, Object> fields) {
        PublicLeadSubmitDTO dto = new PublicLeadSubmitDTO();
        dto.setFormCode(formCode);
        dto.setFields(fields);
        return dto;
    }

    /** 通过反射注入私有字段（替代 Spring @Autowired） */
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
