package com.pengcheng.admin.controller.integration;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.integration.config.IntegrationSyncLog;
import com.pengcheng.integration.service.IntegrationConfigService;
import com.pengcheng.integration.service.IntegrationSyncLogService;
import com.pengcheng.integration.spi.dto.ContactSyncResult;
import com.pengcheng.integration.wecom.WecomImProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通讯录/消息手动同步触发接口。
 */
@Slf4j
@RestController
@RequestMapping("/admin/integration")
@RequiredArgsConstructor
public class IntegrationSyncController {

    private final WecomImProvider          wecomImProvider;
    private final IntegrationConfigService configService;
    private final IntegrationSyncLogService syncLogService;

    /**
     * 手动触发通讯录同步。
     * <p>
     * POST /admin/integration/{provider}/sync?tenantId=xxx
     */
    @PostMapping("/{provider}/sync")
    @SaCheckPermission("integration:sync:trigger")
    public Result<ContactSyncResult> triggerSync(@PathVariable String provider,
                                                  @RequestParam Long tenantId) {
        if (!"wecom".equals(provider)) {
            return Result.fail("暂不支持 provider: " + provider);
        }
        ContactSyncResult result = wecomImProvider.contact().syncContacts(tenantId);
        String status = result.isSuccess() ? "SUCCESS" : "FAILED";
        configService.updateSyncStatus(tenantId, provider, status);
        log.info("[IntegrationSync] provider={} tenantId={} result={}", provider, tenantId, status);
        return Result.ok(result);
    }

    /**
     * 查询最近同步日志。
     * <p>
     * GET /admin/integration/{provider}/logs?tenantId=xxx&limit=20
     */
    @GetMapping("/{provider}/logs")
    @SaCheckPermission("integration:sync:list")
    public Result<List<IntegrationSyncLog>> syncLogs(@PathVariable String provider,
                                                      @RequestParam Long tenantId,
                                                      @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(syncLogService.recentLogs(tenantId, provider, limit));
    }
}
