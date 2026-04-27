package com.pengcheng.system.saas.service;

import com.pengcheng.system.saas.entity.SaasUsageMetric;
import com.pengcheng.system.saas.mapper.SaasUsageMetricMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * SaaS 计量服务。
 *
 * <p>业务方在关键路径调用：
 * <ul>
 *   <li>{@link #incrementApiCalls} — OpenAPI 拦截器每次成功调用 +1</li>
 *   <li>{@link #incrementMau} — 用户登录成功 +1（同月同人去重靠 Redis Set，简化版直接累加）</li>
 *   <li>{@link #recordStorage} — OSS 上传/删除时增减 GB 数</li>
 * </ul>
 *
 * <p>注：高频调用应在 Redis 计数 + 周期 flush 到 DB；当前简化版直接 UPSERT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaasUsageService {

    private final SaasUsageMetricMapper mapper;

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    public void incrementApiCalls(Long tenantId, long delta) {
        if (tenantId == null || delta <= 0) return;
        mapper.upsertIncrement(tenantId, SaasUsageMetric.TYPE_API_CALLS, currentPeriod(), delta);
    }

    public void incrementMau(Long tenantId) {
        if (tenantId == null) return;
        mapper.upsertIncrement(tenantId, SaasUsageMetric.TYPE_MAU, currentPeriod(), 1L);
    }

    public void recordStorage(Long tenantId, long deltaGb) {
        if (tenantId == null) return;
        mapper.upsertIncrement(tenantId, SaasUsageMetric.TYPE_STORAGE_GB, currentPeriod(), deltaGb);
    }

    private String currentPeriod() {
        return LocalDate.now().format(PERIOD_FMT);
    }
}
