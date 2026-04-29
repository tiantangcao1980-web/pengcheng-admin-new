package com.pengcheng.crm.customfield.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "custom_field_def", autoResultMap = true)
public class CustomFieldDef extends BaseEntity {

    /** 实体类型：lead / customer / opportunity ... */
    private String entityType;

    /** 字段 key（同实体内唯一） */
    private String fieldKey;

    /** 显示名 */
    private String label;

    /** 类型：text/number/date/select/multi_select/file */
    private String fieldType;

    /** 是否必填（0/1） */
    private Integer required;

    /** 默认值 */
    private String defaultValue;

    /** 选项 JSON：[{value,label}] */
    private String optionsJson;

    /** 校验 JSON：min/max/pattern/maxLength 等 */
    private String validationJson;

    private Integer sortOrder;

    private Integer enabled;

    private Long tenantId;
}
