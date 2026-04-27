package com.pengcheng.realty.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.crm.customfield.entity.CustomFieldDef;
import com.pengcheng.crm.customfield.mapper.CustomFieldDefMapper;
import com.pengcheng.realty.template.entity.CustomFieldTemplateGroup;
import com.pengcheng.realty.template.mapper.CustomFieldTemplateGroupMapper;
import com.pengcheng.realty.template.service.RealtyFieldTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RealtyFieldTemplateServiceImpl")
class RealtyFieldTemplateServiceImplTest {

    private CustomFieldTemplateGroupMapper groupMapper;
    private CustomFieldDefMapper defMapper;
    private RealtyFieldTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        groupMapper = mock(CustomFieldTemplateGroupMapper.class);
        defMapper   = mock(CustomFieldDefMapper.class);
        service     = new RealtyFieldTemplateServiceImpl(groupMapper, defMapper);
    }

    // ---- 1. applyTemplate 正常路径 ----

    @Test
    @DisplayName("applyTemplate — 启用字段时调用 defMapper.update，set enabled=1")
    void applyTemplate_shouldEnableFields() {
        // 准备
        CustomFieldTemplateGroup group = buildGroup("lead",
                "realty_intent_area,realty_budget,realty_preferred_floor");
        when(groupMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(group);
        when(defMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(3);

        // 执行
        service.applyTemplate("realty", "lead", 1L);

        // 验证
        verify(defMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
    }

    // ---- 2. revokeTemplate 反向 ----

    @Test
    @DisplayName("revokeTemplate — 禁用字段时调用 defMapper.update，set enabled=0")
    void revokeTemplate_shouldDisableFields() {
        CustomFieldTemplateGroup group = buildGroup("customer",
                "realty_intent_area,realty_budget,realty_first_visit_date,realty_decision_role,realty_pay_method");
        when(groupMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(group);
        when(defMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(5);

        service.revokeTemplate("realty", "customer", 1L);

        verify(defMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
    }

    // ---- 3. listGroups ----

    @Test
    @DisplayName("listGroups — 返回该插件所有模板分组")
    void listGroups_shouldReturnAllGroupsForPlugin() {
        List<CustomFieldTemplateGroup> mockGroups = List.of(
                buildGroup("customer",    "realty_budget"),
                buildGroup("lead",        "realty_intent_area"),
                buildGroup("opportunity", "realty_unit_id")
        );
        when(groupMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockGroups);

        List<CustomFieldTemplateGroup> result = service.listGroups("realty");

        assertThat(result).hasSize(3);
        verify(groupMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    // ---- 4. 插件不存在时抛 IllegalArgumentException ----

    @Test
    @DisplayName("applyTemplate — pluginCode+entityType 无对应分组时抛 IllegalArgumentException")
    void applyTemplate_unknownPlugin_throwsIllegalArgument() {
        when(groupMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.applyTemplate("unknown_plugin", "lead", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未找到插件字段模板分组");

        verifyNoInteractions(defMapper);
    }

    // ---- 工具方法 ----

    private static CustomFieldTemplateGroup buildGroup(String entityType, String fieldKeys) {
        return CustomFieldTemplateGroup.builder()
                .id(1L)
                .pluginCode("realty")
                .entityType(entityType)
                .templateName("测试模板")
                .fieldKeys(fieldKeys)
                .build();
    }
}
