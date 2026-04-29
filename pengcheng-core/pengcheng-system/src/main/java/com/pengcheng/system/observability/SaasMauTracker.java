package com.pengcheng.system.observability;

import com.pengcheng.common.context.TenantContextHolder;
import com.pengcheng.system.saas.service.SaasUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * SaaS MAU（月活跃用户）埋点追踪器。
 *
 * <p>监听 Spring 应用内发布的 {@link UserLoginEvent} 事件（由 LoginHelper 发布），
 * 异步调用 {@link SaasUsageService#incrementMau} 累加当租户本月登录次数。
 *
 * <p>设计说明：
 * <ul>
 *   <li>使用 {@code @Async} 避免阻塞登录主路径；</li>
 *   <li>tenantId 由调用方通过 {@link UserLoginEvent} 携带，不依赖 ThreadLocal（跨线程安全）；</li>
 *   <li>高频去重（同用户同月只计 1 次）属后续优化，当前简化版直接累加。</li>
 * </ul>
 *
 * <h3>接入方式</h3>
 * 在登录成功处（如 LoginHelper.doLogin）添加：
 * <pre>{@code
 *   applicationContext.publishEvent(new UserLoginEvent(this, user.getId(), tenantId));
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaasMauTracker {

    private final SaasUsageService saasUsageService;

    /**
     * 用户登录成功事件。
     *
     * <p>内部静态类，避免引入外部 DTO 依赖。仅携带 userId ��� tenantId 两个��段。
     */
    public static class UserLoginEvent extends org.springframework.context.ApplicationEvent {

        private final Long userId;
        private final Long tenantId;

        public UserLoginEvent(Object source, Long userId, Long tenantId) {
            super(source);
            this.userId = userId;
            this.tenantId = tenantId;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getTenantId() {
            return tenantId;
        }
    }

    /**
     * 监听 {@link UserLoginEvent}，异步累加 MAU。
     *
     * @param event 登录事件，包含 userId 和 tenantId
     */
    @Async
    @EventListener
    public void onUserLogin(UserLoginEvent event) {
        Long tenantId = event.getTenantId();
        if (tenantId == null) {
            log.debug("[SaasMauTracker] 登录事件无 tenantId，跳过 MAU 埋点 userId={}", event.getUserId());
            return;
        }
        try {
            saasUsageService.incrementMau(tenantId);
            log.debug("[SaasMauTracker] MAU +1 tenantId={} userId={}", tenantId, event.getUserId());
        } catch (Exception ex) {
            // 计量失败不影响主流程
            log.warn("[SaasMauTracker] MAU 埋点失败 tenantId={} userId={} err={}",
                    tenantId, event.getUserId(), ex.getMessage());
        }
    }
}
