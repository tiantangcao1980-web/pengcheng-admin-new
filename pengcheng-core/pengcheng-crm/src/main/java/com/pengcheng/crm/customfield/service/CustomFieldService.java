package com.pengcheng.crm.customfield.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.common.exception.BizErrorCode;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.customfield.entity.CustomFieldDef;
import com.pengcheng.crm.customfield.entity.CustomFieldValue;
import com.pengcheng.crm.customfield.mapper.CustomFieldDefMapper;
import com.pengcheng.crm.customfield.mapper.CustomFieldValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义字段：定义 CRUD + 值的批量读写。
 */
@Service
public class CustomFieldService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private CustomFieldDefMapper defMapper;

    @Autowired
    private CustomFieldValueMapper valueMapper;

    /* ---------- 定义 ---------- */

    public CustomFieldDef createDef(CustomFieldDef def) {
        if (def.getEntityType() == null || def.getFieldKey() == null || def.getFieldType() == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "entityType/fieldKey/fieldType 必填");
        }
        if (!CustomFieldValidator.SUPPORTED_TYPES.contains(def.getFieldType())) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "不支持的字段类型: " + def.getFieldType());
        }
        Long count = defMapper.selectCount(new LambdaQueryWrapper<CustomFieldDef>()
                .eq(CustomFieldDef::getEntityType, def.getEntityType())
                .eq(CustomFieldDef::getFieldKey, def.getFieldKey()));
        if (count != null && count > 0) {
            throw new BusinessException(BizErrorCode.BUSINESS_ERROR, "字段 key 已存在");
        }
        if (def.getEnabled() == null) def.setEnabled(1);
        if (def.getRequired() == null) def.setRequired(0);
        if (def.getSortOrder() == null) def.setSortOrder(100);
        defMapper.insert(def);
        return def;
    }

    public List<CustomFieldDef> listDefs(String entityType) {
        return defMapper.selectList(new LambdaQueryWrapper<CustomFieldDef>()
                .eq(CustomFieldDef::getEntityType, entityType)
                .eq(CustomFieldDef::getEnabled, 1)
                .orderByAsc(CustomFieldDef::getSortOrder));
    }

    /* ---------- 值 ---------- */

    /**
     * 批量保存某实体的所有自定义字段值。
     * 校验规则：
     *  1) 遍历 defs
     *  2) 用 CustomFieldValidator 逐个校验
     *  3) 任何字段失败 -> 抛 BusinessException 列出第一个错误
     *  4) 落库：删除旧值 -> 重新插入（简化版策略）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveValues(String entityType, Long entityId, Map<String, Object> values) {
        if (entityType == null || entityId == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "entityType/entityId 必填");
        }
        List<CustomFieldDef> defs = listDefs(entityType);
        Map<String, Object> safeValues = values == null ? new HashMap<>() : values;
        for (CustomFieldDef def : defs) {
            String err = CustomFieldValidator.validate(def, safeValues.get(def.getFieldKey()));
            if (err != null) {
                throw new BusinessException(BizErrorCode.BAD_REQUEST, err);
            }
        }

        // 删除旧值
        valueMapper.delete(new LambdaQueryWrapper<CustomFieldValue>()
                .eq(CustomFieldValue::getEntityType, entityType)
                .eq(CustomFieldValue::getEntityId, entityId));

        // 重新插入
        LocalDateTime now = LocalDateTime.now();
        for (CustomFieldDef def : defs) {
            Object v = safeValues.get(def.getFieldKey());
            if (v == null) continue;
            CustomFieldValue cv = CustomFieldValue.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .fieldId(def.getId())
                    .fieldKey(def.getFieldKey())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            assignByType(cv, def.getFieldType(), v);
            valueMapper.insert(cv);
        }
    }

    public Map<String, Object> loadValues(String entityType, Long entityId) {
        List<CustomFieldValue> rows = valueMapper.selectList(new LambdaQueryWrapper<CustomFieldValue>()
                .eq(CustomFieldValue::getEntityType, entityType)
                .eq(CustomFieldValue::getEntityId, entityId));
        Map<String, Object> result = new HashMap<>();
        for (CustomFieldValue cv : rows) {
            if (cv.getValueText() != null) result.put(cv.getFieldKey(), cv.getValueText());
            else if (cv.getValueNumber() != null) result.put(cv.getFieldKey(), cv.getValueNumber());
            else if (cv.getValueDate() != null) result.put(cv.getFieldKey(), cv.getValueDate());
            else if (cv.getValueJson() != null) {
                try {
                    result.put(cv.getFieldKey(), MAPPER.readValue(cv.getValueJson(), Object.class));
                } catch (Exception ignored) {
                    result.put(cv.getFieldKey(), cv.getValueJson());
                }
            }
        }
        return result;
    }

    private static void assignByType(CustomFieldValue cv, String type, Object v) {
        try {
            switch (type) {
                case "text":
                case "select":
                    cv.setValueText(String.valueOf(v));
                    break;
                case "number":
                    cv.setValueNumber(new BigDecimal(String.valueOf(v)));
                    break;
                case "date":
                    String s = String.valueOf(v).replace(' ', 'T');
                    if (s.length() <= 10) s = s + "T00:00:00";
                    cv.setValueDate(LocalDateTime.parse(s));
                    break;
                case "multi_select":
                case "file":
                    if (v instanceof Collection<?> || v instanceof Map<?, ?>) {
                        cv.setValueJson(MAPPER.writeValueAsString(v));
                    } else {
                        cv.setValueJson(String.valueOf(v));
                    }
                    break;
                default:
                    cv.setValueText(String.valueOf(v));
            }
        } catch (Exception e) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "字段值序列化失败: " + e.getMessage());
        }
    }
}
