package com.pengcheng.system.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OpenAPI 密钥（V71）。
 *
 * <p>第三方开发者通过 access_key + secret_key 调用 /openapi/* 端点。
 * secret_key 仅在创建时一次性返回，DB 仅存 SHA256 摘要。
 */
@Data
@TableName("openapi_key")
public class OpenapiKey implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String name;
    private String accessKey;
    private String secretKeyHash;
    private String secretPreview;
    private String scopes;
    private Integer rateLimit;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private String lastUsedIp;
    private Integer enabled;
    private Long createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
