package com.pengcheng.system.openapi;

import com.pengcheng.system.openapi.entity.OpenapiKey;
import com.pengcheng.system.openapi.interceptor.OpenapiAuthInterceptor;
import com.pengcheng.system.openapi.limit.OpenapiRateLimiter;
import com.pengcheng.system.openapi.scope.ScopeChecker;
import com.pengcheng.system.openapi.service.OpenapiCallLogService;
import com.pengcheng.system.openapi.service.OpenapiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenapiAuthInterceptor")
class OpenapiAuthInterceptorTest {

    @Mock private OpenapiKeyService keyService;
    @Mock private OpenapiRateLimiter rateLimiter;
    @Mock private ScopeChecker scopeChecker;
    @Mock private OpenapiCallLogService callLogService;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private OpenapiAuthInterceptor interceptor;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new OpenapiAuthInterceptor(keyService, rateLimiter, scopeChecker, callLogService);
        ReflectionTestUtils.setField(interceptor, "redis", redis);
        responseWriter = new StringWriter();
        // lenient：部分测试用不到 getWriter()，避免 UnnecessaryStubbingException
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        lenient().when(request.getRequestURI()).thenReturn("/openapi/v1/customers");
    }

    @Test
    @DisplayName("缺少请求头 → 401 missing headers")
    void missingHeaders_returns401() throws Exception {
        when(request.getHeader("X-Openapi-Access-Key")).thenReturn(null);
        when(request.getHeader("X-Openapi-Timestamp")).thenReturn(null);
        when(request.getHeader("X-Openapi-Nonce")).thenReturn(null);
        when(request.getHeader("X-Openapi-Signature")).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("时间戳在 ±5min 内 → 通过时间戳检查")
    void timestampWithinTolerance_passes() throws Exception {
        long ts = Instant.now().getEpochSecond();
        when(request.getHeader("X-Openapi-Access-Key")).thenReturn("ak_test");
        when(request.getHeader("X-Openapi-Timestamp")).thenReturn(String.valueOf(ts));
        when(request.getHeader("X-Openapi-Nonce")).thenReturn("nonce1");
        when(request.getHeader("X-Openapi-Signature")).thenReturn("sig");
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(keyService.verifySignature(anyString(), anyString(), anyString())).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, null);

        // 验签失败所以 false，但不是因为时间戳
        verify(response).setStatus(401);
        // 响应内容不是 timestamp out of tolerance
        assertThat(responseWriter.toString()).doesNotContain("timestamp");
    }

    @Test
    @DisplayName("时间戳超出 ±5min → 401 timestamp out of tolerance")
    void timestampOutOfTolerance_returns401() throws Exception {
        long staleTs = Instant.now().getEpochSecond() - 400; // 超出 300s 限制
        when(request.getHeader("X-Openapi-Access-Key")).thenReturn("ak_test");
        when(request.getHeader("X-Openapi-Timestamp")).thenReturn(String.valueOf(staleTs));
        when(request.getHeader("X-Openapi-Nonce")).thenReturn("nonce");
        when(request.getHeader("X-Openapi-Signature")).thenReturn("sig");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(401);
        assertThat(responseWriter.toString()).contains("timestamp");
    }

    @Test
    @DisplayName("nonce 重放 → Redis SETNX 返回 false → 401 nonce replayed")
    void nonceReplay_returns401() throws Exception {
        long ts = Instant.now().getEpochSecond();
        when(request.getHeader("X-Openapi-Access-Key")).thenReturn("ak_test");
        when(request.getHeader("X-Openapi-Timestamp")).thenReturn(String.valueOf(ts));
        when(request.getHeader("X-Openapi-Nonce")).thenReturn("replay_nonce");
        when(request.getHeader("X-Openapi-Signature")).thenReturn("sig");
        when(redis.opsForValue()).thenReturn(valueOps);
        // SETNX 返回 false（nonce 已存在 → 重放）
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(401);
        assertThat(responseWriter.toString()).contains("nonce");
    }

    @Test
    @DisplayName("限流超额 → 429 rate limit exceeded")
    void rateLimitExceeded_returns429() throws Exception {
        long ts = Instant.now().getEpochSecond();
        String ak = "ak_limited";
        when(request.getHeader("X-Openapi-Access-Key")).thenReturn(ak);
        when(request.getHeader("X-Openapi-Timestamp")).thenReturn(String.valueOf(ts));
        when(request.getHeader("X-Openapi-Nonce")).thenReturn("nonce_rl");
        when(request.getHeader("X-Openapi-Signature")).thenReturn("sig");
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(keyService.verifySignature(eq(ak), anyString(), anyString())).thenReturn(true);
        OpenapiKey key = new OpenapiKey();
        key.setAccessKey(ak);
        key.setRateLimit(10);
        when(keyService.findByAccessKey(ak)).thenReturn(Optional.of(key));
        when(rateLimiter.tryAcquire(ak, 10)).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(429);
    }
}
