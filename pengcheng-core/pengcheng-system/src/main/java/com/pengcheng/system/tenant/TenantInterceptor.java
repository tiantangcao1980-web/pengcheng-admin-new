package com.pengcheng.system.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.pengcheng.common.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 租户行过滤拦截器配置（占位，Phase 6 启用）。
 *
 * <p>当 {@code pengcheng.feature.tenant=true} 时激活，为所有受管表自动追加
 * {@code tenant_id = ?} 过滤条件，实现数据行级隔离。
 *
 * <p><strong>当前状态：</strong>配置开关默认 {@code false}，Bean 不会被注册，
 * {@link com.pengcheng.db.config.MybatisPlusConfig} 的拦截器链不受影响。
 * 启用时请在 {@code MybatisPlusConfig#mybatisPlusInterceptor} 中将此拦截器添加到链首。
 *
 * <p><strong>不受多租户过滤的表（ignoredTables）：</strong>
 * sys_role、sys_menu、sys_dict 等平台级共享表需在 {@code isIgnoreTable} 中豁免。
 */
@Configuration
@ConditionalOnProperty(name = "pengcheng.feature.tenant", havingValue = "true", matchIfMissing = false)
public class TenantInterceptor {

    /**
     * 多租户行过滤内部拦截器。
     *
     * <p>仅在 {@code pengcheng.feature.tenant=true} 时注册。
     * 业务代码通过 {@link TenantContextHolder} 透传 tenantId。
     */
    @Bean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {

            /** 返回当前请求的 tenantId；为 null 时 MyBatis-Plus 会忽略过滤。 */
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.get();
                if (tenantId == null) {
                    return new NullValue();
                }
                return new LongValue(tenantId);
            }

            /** 多租户列名，与 sys_org_invite、tenant 等表的 tenant_id 列名一致。 */
            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            /**
             * 是否忽略该表（不追加 tenant_id 过滤）。
             * 平台级共享表（角色/菜单/字典/部门等）应返回 true。
             */
            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORED_TABLES.contains(tableName.toLowerCase());
            }
        });
    }

    /** 不参与多租户过滤的表名（小写） */
    private static final java.util.Set<String> IGNORED_TABLES = java.util.Set.of(
            "sys_role",
            "sys_menu",
            "sys_role_menu",
            "sys_user_role",
            "sys_dept",
            "sys_dict_type",
            "sys_dict_data",
            "sys_config",
            "tenant",
            "user_login_device"
    );
}
