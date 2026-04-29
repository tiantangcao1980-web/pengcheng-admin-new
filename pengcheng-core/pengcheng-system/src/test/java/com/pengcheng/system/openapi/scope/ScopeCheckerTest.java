package com.pengcheng.system.openapi.scope;

import com.pengcheng.system.openapi.entity.OpenapiKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ScopeChecker")
class ScopeCheckerTest {

    private ScopeChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ScopeChecker();
    }

    private OpenapiKey key(String scopesJson) {
        OpenapiKey k = new OpenapiKey();
        k.setScopes(scopesJson);
        return k;
    }

    @Test
    @DisplayName("'*' 通配 — 任意 scope 请求均通过")
    void wildcard_passesAnyScope() {
        OpenapiKey k = key("[\"*\"]");

        assertThat(checker.check(k, "GET", "/openapi/v1/customers")).isTrue();
        assertThat(checker.check(k, "DELETE", "/openapi/v1/leads/1")).isTrue();
        assertThat(checker.check(k, "POST", "/openapi/v1/approvals")).isTrue();
    }

    @Test
    @DisplayName("'customer:*' 通配 — 该域所有 action 通过")
    void domainWildcard_passesAllActionsForDomain() {
        OpenapiKey k = key("[\"customer:*\"]");

        assertThat(checker.check(k, "GET", "/openapi/v1/customers")).isTrue();
        assertThat(checker.check(k, "POST", "/openapi/v1/customers")).isTrue();
        assertThat(checker.check(k, "DELETE", "/openapi/v1/customers/1")).isTrue();
        // 其他域拒绝
        assertThat(checker.check(k, "GET", "/openapi/v1/leads")).isFalse();
    }

    @Test
    @DisplayName("'customer:read' 精确匹配 — GET 通过，POST 拒绝")
    void exactScope_getPassesPostFails() {
        OpenapiKey k = key("[\"customer:read\"]");

        assertThat(checker.check(k, "GET", "/openapi/v1/customers")).isTrue();
        assertThat(checker.check(k, "POST", "/openapi/v1/customers")).isFalse();
        assertThat(checker.check(k, "DELETE", "/openapi/v1/customers/1")).isFalse();
    }

    @Test
    @DisplayName("路径推断：GET /openapi/v1/customers → customer:read")
    void inferScope_getCustomers_customerRead() {
        String scope = checker.inferScope("GET", "/openapi/v1/customers");
        assertThat(scope).isEqualTo("customer:read");
    }

    @Test
    @DisplayName("复数处理：leads → lead，companies → company")
    void inferScope_plural_singularized() {
        assertThat(checker.inferScope("GET", "/openapi/v1/leads")).isEqualTo("lead:read");
        assertThat(checker.inferScope("POST", "/openapi/v1/companies")).isEqualTo("company:write");
    }

    @Test
    @DisplayName("不在 /openapi/v1/ 前缀路径 → inferScope 返回 null → check 返回 false")
    void checkNonOpenapiPath_returnsFalse() {
        OpenapiKey k = key("[\"*\"]");
        // 非 /openapi/* 路径不走拦截逻辑，但 check 直接判断
        String scope = checker.inferScope("GET", "/api/v1/customers");
        assertThat(scope).isNull();

        // check 对 null required scope 返回 false（保守策略），除非 granted 包含 *
        assertThat(checker.check(key("[\"customer:read\"]"), "GET", "/api/v1/customers")).isFalse();
    }
}
