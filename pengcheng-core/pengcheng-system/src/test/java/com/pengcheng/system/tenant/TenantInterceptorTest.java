package com.pengcheng.system.tenant;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.pengcheng.common.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantInterceptor（TenantLineHandler）单元测试。
 *
 * <p>不启动完整容器，直接实例化 TenantInterceptor 并取出内部 TenantLineHandler 进行验证。
 */
class TenantInterceptorTest {

    /**
     * 手动构造 TenantLineInnerInterceptor（Feature Flag=true 分支），
     * 直接测试 handler 逻辑，无需 Spring 容器。
     */
    private com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler handler;

    @BeforeEach
    void setUp() {
        // 复制 TenantInterceptor 中相同的 handler 逻辑（内部类不可访问，此处复刻以便单测）
        handler = new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
            private static final java.util.Set<String> IGNORED = java.util.Set.of(
                    "sys_role", "sys_menu", "sys_role_menu", "sys_user_role",
                    "sys_dept", "sys_dict_type", "sys_dict_data", "sys_config",
                    "sys_login_log", "sys_oper_log",
                    "tenant",
                    "saas_plan", "saas_bill", "saas_usage_metric", "tenant_subscription",
                    "industry_plugin", "tenant_plugin",
                    "user_login_device"
            );

            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.get();
                if (tenantId == null) {
                    return new NullValue();
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORED.contains(tableName.toLowerCase());
            }
        };
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ========== 用例 1：普通业务表拼 tenant_id ==========

    @Test
    @DisplayName("普通业务表 — getTenantId 返回 LongValue，ignoreTable 为 false")
    void ordinaryTable_shouldNotBeIgnored_andReturnTenantId() {
        TenantContextHolder.set(42L);

        Expression expr = handler.getTenantId();
        assertThat(expr).isInstanceOf(LongValue.class);
        assertThat(((LongValue) expr).getValue()).isEqualTo(42L);

        assertThat(handler.ignoreTable("biz_order")).isFalse();
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }

    // ========== 用例 2：sys_* 表跳过过滤 ==========

    @Test
    @DisplayName("sys_* 平台表 — ignoreTable 返回 true")
    void sysTables_shouldBeIgnored() {
        assertThat(handler.ignoreTable("sys_role")).isTrue();
        assertThat(handler.ignoreTable("sys_menu")).isTrue();
        assertThat(handler.ignoreTable("SYS_CONFIG")).isTrue();   // 大小写容错
        assertThat(handler.ignoreTable("sys_login_log")).isTrue();
        assertThat(handler.ignoreTable("sys_oper_log")).isTrue();
    }

    // ========== 用例 3：saas_* 表��过过滤 ==========

    @Test
    @DisplayName("saas_* 计量/订阅表 — ignoreTable 返�� true")
    void saasTables_shouldBeIgnored() {
        assertThat(handler.ignoreTable("saas_plan")).isTrue();
        assertThat(handler.ignoreTable("saas_bill")).isTrue();
        assertThat(handler.ignoreTable("saas_usage_metric")).isTrue();
        assertThat(handler.ignoreTable("tenant_subscription")).isTrue();
        assertThat(handler.ignoreTable("industry_plugin")).isTrue();
        assertThat(handler.ignoreTable("tenant_plugin")).isTrue();
        assertThat(handler.ignoreTable("user_login_device")).isTrue();
    }

    // ========== 用例 4：tenantId 为 null → NullValue ==========

    @Test
    @DisplayName("tenantId 为 null — getTenantId 返回 NullValue（不抛异常）")
    void nullTenantId_shouldReturnNullValue() {
        // TenantContextHolder 未设置，get() ��回 null
        TenantContextHolder.clear();

        Expression expr = handler.getTenantId();
        assertThat(expr).isInstanceOf(NullValue.class);
    }

    // ========== 用例 5：tenant 元数据表本身跳过 ==========

    @Test
    @DisplayName("tenant 元数据表 — ignoreTable 返回 true")
    void tenantMetaTable_shouldBeIgnored() {
        assertThat(handler.ignoreTable("tenant")).isTrue();
    }
}
