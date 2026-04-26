package com.pengcheng.oa.template;

import com.pengcheng.oa.template.entity.ApprovalTemplate;
import com.pengcheng.oa.template.mapper.ApprovalTemplateMapper;
import com.pengcheng.oa.template.service.impl.ApprovalTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ApprovalTemplateServiceImpl")
class ApprovalTemplateServiceImplTest {

    private ApprovalTemplateMapper templateMapper;
    private ApprovalTemplateServiceImpl service;
    private final AtomicLong idSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        templateMapper = mock(ApprovalTemplateMapper.class);
        doAnswer(inv -> {
            ApprovalTemplate t = inv.getArgument(0);
            t.setId(idSeq.getAndIncrement());
            return 1;
        }).when(templateMapper).insert(any(ApprovalTemplate.class));
        when(templateMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(templateMapper.selectOne(any())).thenReturn(null);
        service = new ApprovalTemplateServiceImpl(templateMapper);
    }

    @Test
    @DisplayName("createTemplate：默认启用 + 校验 code 唯一")
    void create_defaultEnabled() {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setCode("custom1");
        t.setName("自定义");
        t.setCategory(4);

        Long id = service.createTemplate(t);
        assertThat(id).isNotNull();
        assertThat(t.getEnabled()).isEqualTo(1);
    }

    @Test
    @DisplayName("createTemplate：code 已存在抛异常")
    void create_duplicateCode() {
        when(templateMapper.selectOne(any())).thenReturn(new ApprovalTemplate());
        ApprovalTemplate t = new ApprovalTemplate();
        t.setCode("leave");
        t.setName("请假");
        t.setCategory(1);
        assertThatThrownBy(() -> service.createTemplate(t))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("校验：code 空")
    void validate_emptyCode() {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setName("x");
        t.setCategory(1);
        assertThatThrownBy(() -> service.createTemplate(t))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("校验：name 空")
    void validate_emptyName() {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setCode("x");
        t.setCategory(1);
        assertThatThrownBy(() -> service.createTemplate(t))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("校验：category 空")
    void validate_emptyCategory() {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setCode("x");
        t.setName("x");
        assertThatThrownBy(() -> service.createTemplate(t))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateTemplate：缺 ID 抛异常")
    void update_missingId() {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setCode("x");
        t.setName("x");
        t.setCategory(1);
        assertThatThrownBy(() -> service.updateTemplate(t))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateTemplate：成功调用 updateById")
    void update_ok() {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setId(1L);
        t.setCode("x");
        t.setName("x");
        t.setCategory(1);
        service.updateTemplate(t);
        verify(templateMapper).updateById(any(ApprovalTemplate.class));
    }

    @Test
    @DisplayName("getByCode：null 返回 null")
    void getByCode_null() {
        assertThat(service.getByCode(null)).isNull();
    }

    @Test
    @DisplayName("listEnabled / listAll 调用对应 mapper 方法")
    void list_methods() {
        when(templateMapper.selectList(any())).thenReturn(List.of(new ApprovalTemplate()));
        assertThat(service.listEnabled()).hasSize(1);
        assertThat(service.listAll()).hasSize(1);
    }

    @Test
    @DisplayName("deleteTemplate 调用 deleteById")
    void delete_ok() {
        service.deleteTemplate(7L);
        verify(templateMapper).deleteById(7L);
    }
}
