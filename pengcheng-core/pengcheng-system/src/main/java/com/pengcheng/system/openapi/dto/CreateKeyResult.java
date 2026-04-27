package com.pengcheng.system.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 创建/轮换密钥的一次性返回结果。
 * <p>secretKey 字段仅这一次返回；之后只能看 secretPreview。
 */
@Data
@Builder
@AllArgsConstructor
public class CreateKeyResult {
    private Long id;
    private String accessKey;
    private String secretKey;       // 一次性明文返回
    private String secretPreview;
}
