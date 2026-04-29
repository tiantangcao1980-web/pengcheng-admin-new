package com.pengcheng.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.integration.config.IntegrationSyncLog;
import com.pengcheng.integration.config.IntegrationSyncLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步日志写入与查询 Service。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationSyncLogService {

    private final IntegrationSyncLogMapper syncLogMapper;

    /**
     * 写入同步日志。
     */
    public void write(Long tenantId, String provider, String syncType,
                      boolean success, int affected, String errorMsg, int durationMs) {
        IntegrationSyncLog syncLog = new IntegrationSyncLog();
        syncLog.setTenantId(tenantId);
        syncLog.setProvider(provider);
        syncLog.setSyncType(syncType);
        syncLog.setSuccess(success ? 1 : 0);
        syncLog.setAffected(affected);
        syncLog.setErrorMsg(errorMsg);
        syncLog.setDurationMs(durationMs);
        syncLog.setCreateTime(LocalDateTime.now());
        syncLogMapper.insert(syncLog);
    }

    /**
     * 查询租户最近 N 条同步日志。
     */
    public List<IntegrationSyncLog> recentLogs(Long tenantId, String provider, int limit) {
        return syncLogMapper.selectList(
                new LambdaQueryWrapper<IntegrationSyncLog>()
                        .eq(IntegrationSyncLog::getTenantId, tenantId)
                        .eq(provider != null, IntegrationSyncLog::getProvider, provider)
                        .orderByDesc(IntegrationSyncLog::getCreateTime)
                        .last("LIMIT " + limit));
    }

    /**
     * 查询指定时间范围内的日志。
     */
    public List<IntegrationSyncLog> queryLogs(Long tenantId, String provider,
                                               LocalDateTime from, LocalDateTime to) {
        return syncLogMapper.selectList(
                new LambdaQueryWrapper<IntegrationSyncLog>()
                        .eq(IntegrationSyncLog::getTenantId, tenantId)
                        .eq(provider != null, IntegrationSyncLog::getProvider, provider)
                        .ge(from != null, IntegrationSyncLog::getCreateTime, from)
                        .le(to != null, IntegrationSyncLog::getCreateTime, to)
                        .orderByDesc(IntegrationSyncLog::getCreateTime));
    }
}
