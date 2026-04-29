package com.pengcheng.integration.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户在 IM 系统的映射实体（对应 integration_user_mapping）。
 */
@Data
@TableName("integration_user_mapping")
public class IntegrationUserMapping implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 内部系统用户 ID */
    private Long userId;

    /** provider 标识 */
    private String provider;

    /** 外部平台用户 ID */
    private String externalId;

    /** 外部部门 ID 列表（JSON 数组字符串） */
    private String externalDeptIds;

    /** 头像 URL */
    private String avatar;

    private LocalDateTime bindAt;
}
