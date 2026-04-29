package com.pengcheng.integration.spi.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * IM 平台部门信息（通讯录同步用）。
 */
@Data
@Accessors(chain = true)
public class ImDeptInfo {

    /** 外部部门 ID */
    private String externalId;

    /** 外部父部门 ID（根部门为空） */
    private String externalParentId;

    /** 部门名称 */
    private String name;

    /** 排序（部分平台支持） */
    private Integer order;
}
