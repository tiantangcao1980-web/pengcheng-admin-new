package com.pengcheng.system.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pengcheng.system.tenant.dto.TenantRegisterRequest;
import com.pengcheng.system.tenant.dto.TenantRegisterResult;
import com.pengcheng.system.tenant.entity.Tenant;

/**
 * 租户服务
 */
public interface TenantService extends IService<Tenant> {

    /**
     * 企业一分钟注册：创建租户 + 管理员 + 默认部门 + 角色绑定。
     */
    TenantRegisterResult registerTenant(TenantRegisterRequest request);

    /**
     * 通过 code 查询租户
     */
    Tenant getByCode(String code);
}
