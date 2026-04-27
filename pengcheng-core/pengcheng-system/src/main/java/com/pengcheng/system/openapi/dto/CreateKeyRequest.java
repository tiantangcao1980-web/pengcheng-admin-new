package com.pengcheng.system.openapi.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 创建 API Key 请求。 */
@Data
public class CreateKeyRequest {
    private String name;
    private List<String> scopes;
    private Integer rateLimit;
    private LocalDateTime expiresAt;
}
