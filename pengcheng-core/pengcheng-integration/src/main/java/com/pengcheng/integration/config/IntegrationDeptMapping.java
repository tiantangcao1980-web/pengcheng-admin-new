package com.pengcheng.integration.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门映射实体（对应 integration_dept_mapping）。
 */
@Data
@TableName("integration_dept_mapping")
public class IntegrationDeptMapping implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 内部系统部门 ID */
    private Long deptId;

    /** provider 标识 */
    private String provider;

    /** 外部部门 ID */
    private String externalId;

    /** 外部父部门 ID */
    private String externalParentId;

    private LocalDateTime createTime;
}
