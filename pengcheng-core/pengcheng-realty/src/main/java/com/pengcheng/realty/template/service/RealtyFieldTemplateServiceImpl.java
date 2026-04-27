package com.pengcheng.realty.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.crm.customfield.entity.CustomFieldDef;
import com.pengcheng.crm.customfield.mapper.CustomFieldDefMapper;
import com.pengcheng.realty.template.entity.CustomFieldTemplateGroup;
import com.pengcheng.realty.template.mapper.CustomFieldTemplateGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * {@link RealtyFieldTemplateService} 实现。
 *
 * <p><b>多租户说明（TODO Phase-6）</b>：
 * 当前实现操作全局 custom_field_def（无 tenant 分区），tenantId 参数仅做日志占位。
 * Phase 6 在 custom_field_def 增加 tenant_id 列后，需在 Wrapper 追加 tenant 过滤。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RealtyFieldTemplateServiceImpl implements RealtyFieldTemplateService {

    private final CustomFieldTemplateGroupMapper groupMapper;
    private final CustomFieldDefMapper            defMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyTemplate(String pluginCode, String entityType, Long tenantId) {
        CustomFieldTemplateGroup group = requireGroup(pluginCode, entityType);
        List<String> keys = splitKeys(group.getFieldKeys());
        if (keys.isEmpty()) {
            return;
        }
        // TODO(Phase-6): 增加 tenantId 过滤条件，实现租户级字段隔离
        defMapper.update(null, new LambdaUpdateWrapper<CustomFieldDef>()
                .in(CustomFieldDef::getFieldKey, keys)
                .eq(CustomFieldDef::getEntityType, entityType)
                .set(CustomFieldDef::getEnabled, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeTemplate(String pluginCode, String entityType, Long tenantId) {
        CustomFieldTemplateGroup group = requireGroup(pluginCode, entityType);
        List<String> keys = splitKeys(group.getFieldKeys());
        if (keys.isEmpty()) {
            return;
        }
        // TODO(Phase-6): 增加 tenantId 过滤条件，实现租户级字段隔离
        defMapper.update(null, new LambdaUpdateWrapper<CustomFieldDef>()
                .in(CustomFieldDef::getFieldKey, keys)
                .eq(CustomFieldDef::getEntityType, entityType)
                .set(CustomFieldDef::getEnabled, 0));
    }

    @Override
    public List<CustomFieldTemplateGroup> listGroups(String pluginCode) {
        return groupMapper.selectList(new LambdaQueryWrapper<CustomFieldTemplateGroup>()
                .eq(CustomFieldTemplateGroup::getPluginCode, pluginCode)
                .orderByAsc(CustomFieldTemplateGroup::getEntityType));
    }

    // ---- 私有辅助 ----

    private CustomFieldTemplateGroup requireGroup(String pluginCode, String entityType) {
        CustomFieldTemplateGroup group = groupMapper.selectOne(new LambdaQueryWrapper<CustomFieldTemplateGroup>()
                .eq(CustomFieldTemplateGroup::getPluginCode, pluginCode)
                .eq(CustomFieldTemplateGroup::getEntityType, entityType));
        if (group == null) {
            throw new IllegalArgumentException(
                    String.format("未找到插件字段模板分组：pluginCode=%s, entityType=%s", pluginCode, entityType));
        }
        return group;
    }

    private static List<String> splitKeys(String fieldKeys) {
        if (!StringUtils.hasText(fieldKeys)) {
            return List.of();
        }
        return Arrays.stream(fieldKeys.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
