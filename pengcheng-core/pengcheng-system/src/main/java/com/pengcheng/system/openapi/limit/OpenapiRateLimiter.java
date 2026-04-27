package com.pengcheng.system.openapi.limit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * OpenAPI 限流器（按 AK + 当前分钟窗口）。
 *
 * <p>Redis 不可用时降级允许通过（避免限流器故障导致 API 全挂），打 WARN。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenapiRateLimiter {

    @Autowired(required = false)
    private StringRedisTemplate redis;

    private static final DateTimeFormatter MIN_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * @return true 通过；false 超限
     */
    public boolean tryAcquire(String accessKey, int limit) {
        if (redis == null) {
            log.warn("[OpenapiRateLimiter] Redis 未配置，限流降级为通过");
            return true;
        }
        try {
            String key = "openapi:rl:" + accessKey + ":" + LocalDateTime.now().format(MIN_FMT);
            Long count = redis.opsForValue().increment(key);
            if (count == null) return true;
            if (count == 1L) {
                redis.expire(key, Duration.ofSeconds(70));
            }
            return count <= limit;
        } catch (Exception e) {
            log.warn("[OpenapiRateLimiter] Redis 异常，限流降级为通过: {}", e.getMessage());
            return true;
        }
    }
}
