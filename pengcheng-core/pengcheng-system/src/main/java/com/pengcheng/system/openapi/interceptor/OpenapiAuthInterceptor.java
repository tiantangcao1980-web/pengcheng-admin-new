package com.pengcheng.system.openapi.interceptor;

import com.pengcheng.system.openapi.entity.OpenapiKey;
import com.pengcheng.system.openapi.limit.OpenapiRateLimiter;
import com.pengcheng.system.openapi.service.OpenapiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * OpenAPI 拦截器：验签 + 限流 + 防重放。
 *
 * <p>请求头：
 * <ul>
 *   <li>X-Openapi-Access-Key</li>
 *   <li>X-Openapi-Timestamp（unix 秒，±5 分钟容差）</li>
 *   <li>X-Openapi-Nonce（5 分钟内唯一）</li>
 *   <li>X-Openapi-Signature = Base64(HMAC-SHA256(SK, stringToSign))</li>
 * </ul>
 *
 * <p>stringToSign = method + "\n" + path + "\n" + timestamp + "\n" + nonce
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenapiAuthInterceptor implements HandlerInterceptor {

    private final OpenapiKeyService keyService;
    private final OpenapiRateLimiter rateLimiter;

    @Autowired(required = false)
    private StringRedisTemplate redis;

    private static final long TIMESTAMP_TOLERANCE_SEC = 300;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if (!req.getRequestURI().startsWith("/openapi/")) {
            return true;
        }

        String ak = req.getHeader("X-Openapi-Access-Key");
        String ts = req.getHeader("X-Openapi-Timestamp");
        String nonce = req.getHeader("X-Openapi-Nonce");
        String sig = req.getHeader("X-Openapi-Signature");

        if (ak == null || ts == null || nonce == null || sig == null) {
            return reject(resp, 401, "missing openapi headers");
        }

        // 时间戳容差
        try {
            long t = Long.parseLong(ts);
            if (Math.abs(Instant.now().getEpochSecond() - t) > TIMESTAMP_TOLERANCE_SEC) {
                return reject(resp, 401, "timestamp out of tolerance");
            }
        } catch (NumberFormatException e) {
            return reject(resp, 401, "bad timestamp");
        }

        // nonce 防重放（Redis SETNX 5min）
        if (redis != null) {
            String nKey = "openapi:nonce:" + ak + ":" + nonce;
            Boolean ok = redis.opsForValue().setIfAbsent(nKey, "1", Duration.ofMinutes(5));
            if (!Boolean.TRUE.equals(ok)) {
                return reject(resp, 401, "nonce replayed");
            }
        }

        // 验签
        String stringToSign = req.getMethod() + "\n" + req.getRequestURI() + "\n" + ts + "\n" + nonce;
        if (!keyService.verifySignature(ak, sig, stringToSign)) {
            return reject(resp, 401, "signature invalid");
        }

        // 限流
        Optional<OpenapiKey> kOpt = keyService.findByAccessKey(ak);
        if (kOpt.isEmpty()) {
            return reject(resp, 401, "ak not found");
        }
        OpenapiKey k = kOpt.get();
        if (!rateLimiter.tryAcquire(ak, k.getRateLimit())) {
            return reject(resp, 429, "rate limit exceeded");
        }

        OpenapiContext.set(k);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        OpenapiContext.clear();
    }

    private boolean reject(HttpServletResponse resp, int status, String msg) throws Exception {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":" + status + ",\"message\":\"" + msg + "\"}");
        return false;
    }
}
