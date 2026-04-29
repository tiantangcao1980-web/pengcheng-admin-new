package com.pengcheng.system.observability;

import com.pengcheng.system.saas.service.SaasUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * SaasMauTracker 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SaasMauTrackerTest {

    @Mock
    private SaasUsageService saasUsageService;

    @InjectMocks
    private SaasMauTracker tracker;

    // ========== 用例 1：有 tenantId → 调用 incrementMau ==========

    @Test
    @DisplayName("tenantId 存在 — 调用 SaasUsageService.incrementMau")
    void givenTenantId_shouldIncrementMau() {
        SaasMauTracker.UserLoginEvent event =
                new SaasMauTracker.UserLoginEvent(this, 100L, 7L);

        tracker.onUserLogin(event);

        verify(saasUsageService, times(1)).incrementMau(7L);
    }

    // ========== 用例 2：tenantId 为 null → 跳过，不调用 incrementMau ==========

    @Test
    @DisplayName("tenantId 为 null — 跳过计量，不调用 SaasUsageService")
    void givenNullTenantId_shouldSkipMau() {
        SaasMauTracker.UserLoginEvent event =
                new SaasMauTracker.UserLoginEvent(this, 200L, null);

        tracker.onUserLogin(event);

        verifyNoInteractions(saasUsageService);
    }
}
