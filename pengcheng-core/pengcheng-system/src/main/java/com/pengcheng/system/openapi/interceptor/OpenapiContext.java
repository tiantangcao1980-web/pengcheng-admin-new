package com.pengcheng.system.openapi.interceptor;

import com.pengcheng.system.openapi.entity.OpenapiKey;

/**
 * 当前 OpenAPI 调用上下文（ThreadLocal）。
 *
 * <p>由 {@link OpenapiAuthInterceptor} 在验签通过后写入，业务 Controller 可读取
 * 调用方 tenantId / accessKey / scopes 做精细化授权或租户隔离。
 */
public final class OpenapiContext {

    private static final ThreadLocal<OpenapiKey> CURRENT = new ThreadLocal<>();

    private OpenapiContext() {}

    public static void set(OpenapiKey key) {
        CURRENT.set(key);
    }

    public static OpenapiKey get() {
        return CURRENT.get();
    }

    public static Long currentTenantId() {
        OpenapiKey k = CURRENT.get();
        return k != null ? k.getTenantId() : null;
    }

    public static String currentAccessKey() {
        OpenapiKey k = CURRENT.get();
        return k != null ? k.getAccessKey() : null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
