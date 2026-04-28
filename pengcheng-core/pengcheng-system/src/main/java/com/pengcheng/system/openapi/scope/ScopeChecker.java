package com.pengcheng.system.openapi.scope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.openapi.entity.OpenapiKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenAPI scope 校验器（M3）。
 *
 * <p>scopes 存储为 JSON 数组字符串，例：{@code ["customer:read","crm:write","approval:*"]}。
 * <ul>
 *   <li>每个 scope 形式 {@code domain:action}</li>
 *   <li>{@code "*"} 通配 — 任意 scope 通过</li>
 *   <li>{@code "domain:*"} 通配 — 该域所有 action 通过</li>
 * </ul>
 *
 * <p>请求路径到 scope 的映射约定（path → required scope）：
 * <pre>
 *   GET    /openapi/v1/customers     →  customer:read
 *   POST   /openapi/v1/customers     →  customer:write
 *   PUT    /openapi/v1/customers/x   →  customer:write
 *   DELETE /openapi/v1/customers/x   →  customer:delete
 *   GET    /openapi/v1/leads         →  lead:read
 *   POST   /openapi/v1/approvals     →  approval:write
 * </pre>
 */
@Slf4j
@Component
public class ScopeChecker {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @return true 通过；false 缺权
     */
    public boolean check(OpenapiKey key, String method, String path) {
        if (key == null) return false;
        Set<String> granted = parseScopes(key.getScopes());
        if (granted.contains("*")) return true;

        String required = inferScope(method, path);
        if (required == null) {
            // 路径不在白名单 — 默认拒绝（保守策略）
            log.warn("[ScopeChecker] 未识别 scope，拒绝：{} {}", method, path);
            return false;
        }
        if (granted.contains(required)) return true;

        // 检查 domain:* 通配
        int colon = required.indexOf(':');
        if (colon > 0) {
            String wildcard = required.substring(0, colon) + ":*";
            if (granted.contains(wildcard)) return true;
        }
        return false;
    }

    Set<String> parseScopes(String json) {
        if (json == null || json.isBlank()) return new HashSet<>();
        try {
            List<String> list = objectMapper.readValue(json, List.class);
            return new HashSet<>(list);
        } catch (Exception e) {
            log.warn("[ScopeChecker] scopes JSON 解析失败：{}", json);
            return new HashSet<>();
        }
    }

    /** 从 method + path 推断 required scope。 */
    String inferScope(String method, String path) {
        if (path == null) return null;
        // /openapi/v1/{domain} 或 /openapi/v1/{domain}/{id}/...
        if (!path.startsWith("/openapi/v1/")) return null;
        String tail = path.substring("/openapi/v1/".length());
        int slash = tail.indexOf('/');
        String domain = (slash > 0) ? tail.substring(0, slash) : tail;
        if (domain.isEmpty()) return null;
        // 去复数 s（customers → customer / leads → lead）
        if (domain.endsWith("ies")) {
            domain = domain.substring(0, domain.length() - 3) + "y";
        } else if (domain.endsWith("s")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        String action;
        switch (method.toUpperCase()) {
            case "GET":
            case "HEAD":
                action = "read";
                break;
            case "POST":
            case "PUT":
            case "PATCH":
                action = "write";
                break;
            case "DELETE":
                action = "delete";
                break;
            default:
                return null;
        }
        return domain + ":" + action;
    }
}
