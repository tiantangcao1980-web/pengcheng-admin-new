package com.pengcheng.crm.tag.entity;

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
@TableName("customer_tag")
public class CustomerTag extends BaseEntity {
    private String tagName;
    private String color;
    private String category;
    private String description;
    private Integer sortOrder;
    private Integer enabled;
    private Long tenantId;
}
