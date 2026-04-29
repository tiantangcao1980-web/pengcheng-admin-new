package com.pengcheng.db.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.pengcheng.db.interceptor.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置
 */
@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * 可选注入 TenantLineInnerInterceptor（仅 pengcheng.feature.tenant=true 时由
     * {@code TenantInterceptor} 配置类注册该 Bean，否则为 null）。
     */
    @Autowired(required = false)
    private TenantLineInnerInterceptor tenantLineInnerInterceptor;

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataPermissionInterceptor dataPermissionInterceptor) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 0. 多租户行过滤（链首，仅 pengcheng.feature.tenant=true 时注入）
        if (tenantLineInnerInterceptor != null) {
            interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        }
        // 1. 数据权限插件（先执行 SQL 包装）
        interceptor.addInnerInterceptor(dataPermissionInterceptor);
        // 2. 分页插件（最后执行，确保 LIMIT 在最外层）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        try {
            if (StpUtil.isLogin()) {
                this.strictInsertFill(metaObject, "createBy", Long.class, StpUtil.getLoginIdAsLong());
                this.strictInsertFill(metaObject, "updateBy", Long.class, StpUtil.getLoginIdAsLong());
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        try {
            if (StpUtil.isLogin()) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, StpUtil.getLoginIdAsLong());
            }
        } catch (Exception ignored) {
        }
    }
}
