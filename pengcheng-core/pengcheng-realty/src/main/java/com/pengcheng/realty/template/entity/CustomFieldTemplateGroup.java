package com.pengcheng.realty.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行业字段模板分组。
 * 每条记录描述一个行业插件在某实体类型下批量启用/禁用的字段列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("custom_field_template_group")
public class CustomFieldTemplateGroup implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 行业插件编码，如 realty */
    private String pluginCode;

    /** 实体类型：lead / customer / opportunity */
    private String entityType;

    /** 模板名称 */
    private String templateName;

    /** 逗号分隔的 field_key 列表 */
    private String fieldKeys;

    private LocalDateTime createTime;
}
