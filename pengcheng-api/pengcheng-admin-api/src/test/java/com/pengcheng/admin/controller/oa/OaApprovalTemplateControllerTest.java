package com.pengcheng.admin.controller.oa;

import com.pengcheng.common.result.Result;
import com.pengcheng.oa.template.entity.ApprovalTemplate;
import com.pengcheng.oa.template.service.ApprovalTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OaApprovalTemplateController 单元测试。
 * 使用 MockitoExtension，不启动 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OaApprovalTemplateController 单元测试")
class OaApprovalTemplateControllerTest {

    @Mock
    private ApprovalTemplateService templateService;

    @InjectMocks
    private OaApprovalTemplateController controller;

    // -------- 辅助工厂方法 --------

    private ApprovalTemplate buildTemplate(Long id, String code, String name) {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setId(id);
        t.setCode(code);
        t.setName(name);
        t.setCategory(1);
        t.setEnabled(1);
        t.setFormSchema("{\"fields\":[]}");
        return t;
    }

    // -------- list --------

    @Test
    @DisplayName("list — enabledOnly=null 返回全部模板")
    void list_all_returnsAll() {
        List<ApprovalTemplate> all = Arrays.asList(
                buildTemplate(1L, ApprovalTemplate.CODE_LEAVE, "请假"),
                buildTemplate(2L, ApprovalTemplate.CODE_OUTING, "外出")
        );
        when(templateService.listAll()).thenReturn(all);

        Result<List<ApprovalTemplate>> result = controller.list(null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
        verify(templateService).listAll();
        verify(templateService, never()).listEnabled();
    }

    @Test
    @DisplayName("list — enabledOnly=true 只返回启用模板")
    void list_enabledOnly_returnsEnabled() {
        List<ApprovalTemplate> enabled = Collections.singletonList(
                buildTemplate(1L, ApprovalTemplate.CODE_LEAVE, "请假")
        );
        when(templateService.listEnabled()).thenReturn(enabled);

        Result<List<ApprovalTemplate>> result = controller.list(true);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
        verify(templateService).listEnabled();
        verify(templateService, never()).listAll();
    }

    // -------- get by id --------

    @Test
    @DisplayName("get — 按 ID 返回指定模板")
    void get_byId_returnsTemplate() {
        ApprovalTemplate t = buildTemplate(10L, ApprovalTemplate.CODE_OVERTIME, "加班");
        when(templateService.getById(10L)).thenReturn(t);

        Result<ApprovalTemplate> result = controller.get(10L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getCode()).isEqualTo(ApprovalTemplate.CODE_OVERTIME);
    }

    // -------- getByCode --------

    @Test
    @DisplayName("getByCode — 按业务编码返回模板")
    void getByCode_returnsTemplate() {
        ApprovalTemplate t = buildTemplate(5L, ApprovalTemplate.CODE_REIMBURSE, "报销");
        when(templateService.getByCode(ApprovalTemplate.CODE_REIMBURSE)).thenReturn(t);

        Result<ApprovalTemplate> result = controller.getByCode(ApprovalTemplate.CODE_REIMBURSE);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("报销");
    }

    // -------- create --------

    @Test
    @DisplayName("create — 正常创建返回新 ID")
    void create_success_returnsNewId() {
        ApprovalTemplate t = buildTemplate(null, "custom_01", "自定义审批");
        when(templateService.createTemplate(t)).thenReturn(100L);

        Result<Long> result = controller.create(t);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(100L);
    }

    @Test
    @DisplayName("create — Service 抛异常时冒泡（编码重复等）")
    void create_serviceThrows_propagatesException() {
        ApprovalTemplate t = buildTemplate(null, "leave", "重复请假模板");
        when(templateService.createTemplate(t))
                .thenThrow(new IllegalStateException("模板编码已存在：leave"));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> controller.create(t)
        );
    }

    // -------- update --------

    @Test
    @DisplayName("update — 更新成功返回 200")
    void update_success_returnsOk() {
        ApprovalTemplate t = buildTemplate(null, ApprovalTemplate.CODE_GENERAL, "通用 v2");
        doNothing().when(templateService).updateTemplate(any(ApprovalTemplate.class));

        Result<Void> result = controller.update(3L, t);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(t.getId()).isEqualTo(3L);
        verify(templateService).updateTemplate(t);
    }

    // -------- delete --------

    @Test
    @DisplayName("delete — 删除成功返回 200")
    void delete_success_returnsOk() {
        doNothing().when(templateService).deleteTemplate(8L);

        Result<Void> result = controller.delete(8L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(templateService).deleteTemplate(8L);
    }
}
