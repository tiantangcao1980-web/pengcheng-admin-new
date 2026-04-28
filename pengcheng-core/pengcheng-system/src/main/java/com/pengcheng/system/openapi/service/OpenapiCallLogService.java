package com.pengcheng.system.openapi.service;

import com.pengcheng.system.openapi.entity.OpenapiCallLog;
import com.pengcheng.system.openapi.mapper.OpenapiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * OpenapiCallLog 异步落库（M3）。
 *
 * <p>OpenapiAuthInterceptor.afterCompletion 调 {@link #recordAsync}，
 * @Async 异步写库，避免拖慢 API 响应。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenapiCallLogService {

    private final OpenapiCallLogMapper mapper;

    @Async
    public void recordAsync(String accessKey, Long tenantId, String method, String path,
                             int statusCode, String requestId, int durationMs,
                             String requestIp, String errorMsg) {
        try {
            OpenapiCallLog log = OpenapiCallLog.builder()
                    .accessKey(accessKey)
                    .tenantId(tenantId)
                    .method(method)
                    .path(path)
                    .statusCode(statusCode)
                    .requestId(requestId)
                    .durationMs(durationMs)
                    .requestIp(requestIp)
                    .errorMsg(errorMsg != null && errorMsg.length() > 255
                            ? errorMsg.substring(0, 255) : errorMsg)
                    .createTime(LocalDateTime.now())
                    .build();
            mapper.insert(log);
        } catch (Exception e) {
            log.warn("[OpenapiCallLog] 异步落库失败 ak={} path={}", accessKey, path, e);
        }
    }
}
