package com.pengcheng.common.context;

/**
 * 租户上下文持有者。
 *
 * <p>使用 ThreadLocal 在请求生命周期内透传当前租户 ID。
 * 在请求入口（如过滤器/拦截器）处设置，在请求结束时务必调用 {@link #clear()} 以防内存泄漏。
 *
 * <p><strong>使用场景</strong>（Phase 6 启用，当前默认不开启）：
 * <ul>
 *   <li>TenantInterceptor 读取请求头或 Token 中的 tenantId 后调用 {@link #set(Long)}</li>
 *   <li>业务代码通过 {@link #get()} 获取当前租户 ID</li>
 *   <li>MyBatis-Plus TenantLine 插件读取此值自动拼接 tenant_id 条件</li>
 * </ul>
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 设置当前线程的租户 ID。
     *
     * @param tenantId 租户 ID，null 表示无租户上下文
     */
    public static void set(Long tenantId) {
        if (tenantId == null) {
            TENANT_ID_HOLDER.remove();
        } else {
            TENANT_ID_HOLDER.set(tenantId);
        }
    }

    /**
     * 获取当前线程的租户 ID。
     *
     * @return 租户 ID，若未设置则返回 null
     */
    public static Long get() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 清除当前线程的租户上下文，防止 ThreadLocal 内存泄漏。
     * 应在请求结束时（finally 块或 Filter 后置处理）调用。
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
