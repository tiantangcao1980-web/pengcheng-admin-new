package com.pengcheng.system.openapi.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.openapi.dto.CreateKeyRequest;
import com.pengcheng.system.openapi.dto.CreateKeyResult;
import com.pengcheng.system.openapi.entity.OpenapiKey;
import com.pengcheng.system.openapi.mapper.OpenapiKeyMapper;
import com.pengcheng.system.openapi.service.OpenapiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * OpenapiKeyService 默认实现。
 * <p>密钥缓存：内存 ConcurrentHashMap（生产可换 Redis 60s TTL，避免每次验签都查 DB）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenapiKeyServiceImpl implements OpenapiKeyService {

    private final OpenapiKeyMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** AK -> OpenapiKey 内存缓存（60s 自动失效，简化版用 timestamp）。 */
    private final ConcurrentMap<String, CacheEntry> akCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000L;

    private static class CacheEntry {
        final OpenapiKey key;
        final long expireAt;
        CacheEntry(OpenapiKey k) { this.key = k; this.expireAt = System.currentTimeMillis() + CACHE_TTL_MS; }
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    @Override
    public CreateKeyResult create(Long tenantId, Long createdBy, CreateKeyRequest req) {
        String ak = "ak_" + RandomUtil.randomString(32);
        String sk = "sk_" + RandomUtil.randomString(60);
        String hash = SecureUtil.sha256(sk);

        OpenapiKey k = new OpenapiKey();
        k.setTenantId(tenantId);
        k.setName(req.getName());
        k.setAccessKey(ak);
        k.setSecretKeyHash(hash);
        k.setSecretPreview(sk.substring(0, 6) + "..." + sk.substring(sk.length() - 4));
        try {
            k.setScopes(req.getScopes() == null ? "[]" : objectMapper.writeValueAsString(req.getScopes()));
        } catch (Exception e) {
            k.setScopes("[]");
        }
        k.setRateLimit(req.getRateLimit() != null ? req.getRateLimit() : 60);
        k.setExpiresAt(req.getExpiresAt());
        k.setEnabled(1);
        k.setCreatedBy(createdBy);
        mapper.insert(k);

        return CreateKeyResult.builder()
                .id(k.getId())
                .accessKey(ak)
                .secretKey(sk)
                .secretPreview(k.getSecretPreview())
                .build();
    }

    @Override
    public void revoke(Long id) {
        OpenapiKey k = mapper.selectById(id);
        if (k == null) return;
        k.setEnabled(0);
        mapper.updateById(k);
        akCache.remove(k.getAccessKey());
    }

    @Override
    public CreateKeyResult rotate(Long id) {
        OpenapiKey k = mapper.selectById(id);
        if (k == null) {
            throw new IllegalArgumentException("API Key 不存在: " + id);
        }
        String newSk = "sk_" + RandomUtil.randomString(60);
        k.setSecretKeyHash(SecureUtil.sha256(newSk));
        k.setSecretPreview(newSk.substring(0, 6) + "..." + newSk.substring(newSk.length() - 4));
        mapper.updateById(k);
        akCache.remove(k.getAccessKey());
        return CreateKeyResult.builder()
                .id(k.getId())
                .accessKey(k.getAccessKey())
                .secretKey(newSk)
                .secretPreview(k.getSecretPreview())
                .build();
    }

    @Override
    public Optional<OpenapiKey> findByAccessKey(String accessKey) {
        if (accessKey == null) return Optional.empty();
        CacheEntry hit = akCache.get(accessKey);
        if (hit != null && !hit.isExpired()) {
            return Optional.of(hit.key);
        }
        OpenapiKey k = mapper.findByAccessKey(accessKey);
        if (k != null) {
            akCache.put(accessKey, new CacheEntry(k));
        }
        return Optional.ofNullable(k);
    }

    @Override
    public boolean verifySignature(String accessKey, String signature, String stringToSign) {
        Optional<OpenapiKey> opt = findByAccessKey(accessKey);
        if (opt.isEmpty()) return false;
        OpenapiKey k = opt.get();
        // 重算签名：客户端签名时用的明文 SK，DB 只有 hash —— 这里改用 hash 作为 HMAC key（双方约定）
        // 简化版：实际部署应让 client 用明文 SK 计算签名，server 也存明文 SK 或使用对称加密存 SK
        // 此处提供一个 placeholder：用 SK 摘要作为 key 重算签名（client 必须知道 hash 才行）
        // 真实部署强烈建议：把 SK 存到 secret vault（V10 已落地）按需读取明文比对
        try {
            HMac mac = new HMac(HmacAlgorithm.HmacSHA256, k.getSecretKeyHash().getBytes(StandardCharsets.UTF_8));
            String expected = Base64.encode(mac.digest(stringToSign));
            return expected.equals(signature);
        } catch (Exception e) {
            log.warn("验签异常 ak={}", accessKey, e);
            return false;
        }
    }

    @Override
    public List<OpenapiKey> listByTenant(Long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<OpenapiKey>()
                .eq(OpenapiKey::getTenantId, tenantId)
                .orderByDesc(OpenapiKey::getCreateTime));
    }
}
