package com.pengcheng.system.openapi.service;

import com.pengcheng.system.openapi.dto.CreateKeyRequest;
import com.pengcheng.system.openapi.dto.CreateKeyResult;
import com.pengcheng.system.openapi.entity.OpenapiKey;

import java.util.List;
import java.util.Optional;

/**
 * OpenAPI 密钥服务。
 *
 * <p>核心契约：
 * <ul>
 *   <li>{@link #create} 同时生成 AK（32 字符 base62）+ SK（64 字符 base62），DB 存 SK SHA256 摘要；
 *       返回 {@link CreateKeyResult#getSecretKey()} 一次性明文，调用方需立即妥善保存；</li>
 *   <li>{@link #verifySignature} 用存储的 SK 摘要重算 HMAC-SHA256 与请求签名比对；</li>
 *   <li>{@link #findByAccessKey} 用 Redis 缓存 60s 加速验签链路。</li>
 * </ul>
 */
public interface OpenapiKeyService {

    CreateKeyResult create(Long tenantId, Long createdBy, CreateKeyRequest req);

    /** 软禁用（enabled=0），不删除以保留审计链。 */
    void revoke(Long id);

    /** 重置 SK，旧 SK 立即失效；返回一次性明文新 SK。 */
    CreateKeyResult rotate(Long id);

    Optional<OpenapiKey> findByAccessKey(String accessKey);

    /**
     * 用 stringToSign 与存储 SK 计算 HMAC-SHA256 签名比对。
     *
     * @return true 校验通过；false 任何不匹配
     */
    boolean verifySignature(String accessKey, String signature, String stringToSign);

    List<OpenapiKey> listByTenant(Long tenantId);
}
