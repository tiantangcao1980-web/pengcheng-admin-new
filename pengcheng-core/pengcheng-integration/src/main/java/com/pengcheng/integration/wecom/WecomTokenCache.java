package com.pengcheng.integration.wecom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 企业微信 access_token 内存缓存。
 * <p>
 * 策略：双检锁（ReadWriteLock）；过期时间 = 企业微信返回 expires_in（7200s）- 安全边际 300s。
 * 多租户下以 "corpId:secret" 为 key，彼此隔离。
 */
@Slf4j
@Component
public class WecomTokenCache {

    /** 安全边际 300 秒 */
    private static final long SAFETY_MARGIN_MS = 300_000L;

    private static final String TOKEN_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";

    private final Map<String, TokenEntry> cache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock    = new ReentrantReadWriteLock();
    private final WecomHttpClient httpClient;

    public WecomTokenCache(WecomHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 获取有效 access_token，过期时自动刷新。
     */
    public String getToken(String corpId, String secret) {
        String cacheKey = corpId + ":" + secret;

        lock.readLock().lock();
        try {
            TokenEntry entry = cache.get(cacheKey);
            if (entry != null && !entry.isExpired()) {
                return entry.token;
            }
        } finally {
            lock.readLock().unlock();
        }

        // 双检锁写入
        lock.writeLock().lock();
        try {
            TokenEntry entry = cache.get(cacheKey);
            if (entry != null && !entry.isExpired()) {
                return entry.token;
            }
            TokenEntry newEntry = fetchToken(corpId, secret);
            cache.put(cacheKey, newEntry);
            log.info("[WecomToken] refreshed for corpId={}", corpId);
            return newEntry.token;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 强制使指定 corpId 的缓存失效（用于测试或外部触发）。
     */
    public void invalidate(String corpId, String secret) {
        cache.remove(corpId + ":" + secret);
    }

    private TokenEntry fetchToken(String corpId, String secret) {
        String url = String.format(TOKEN_URL, corpId, secret);
        Map<String, Object> resp = httpClient.get(url);
        String token     = (String) resp.get("access_token");
        int    expiresIn = ((Number) resp.get("expires_in")).intValue();
        long   expireAt  = System.currentTimeMillis() + (expiresIn * 1000L) - SAFETY_MARGIN_MS;
        return new TokenEntry(token, expireAt);
    }

    // ---- inner ----

    static final class TokenEntry {
        final String token;
        final long   expireAt;  // epoch ms

        TokenEntry(String token, long expireAt) {
            this.token    = token;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expireAt;
        }
    }
}
