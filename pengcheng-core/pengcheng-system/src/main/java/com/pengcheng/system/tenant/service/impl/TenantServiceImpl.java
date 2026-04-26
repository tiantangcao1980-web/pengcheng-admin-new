package com.pengcheng.system.tenant.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysRole;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.mapper.SysUserRoleMapper;
import com.pengcheng.system.service.SysDeptService;
import com.pengcheng.system.service.SysRoleService;
import com.pengcheng.system.service.SysUserService;
import com.pengcheng.system.tenant.dto.TenantRegisterRequest;
import com.pengcheng.system.tenant.dto.TenantRegisterResult;
import com.pengcheng.system.tenant.entity.Tenant;
import com.pengcheng.system.tenant.mapper.TenantMapper;
import com.pengcheng.system.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 租户服务实现
 *
 * <p>核心业务：企业一分钟注册，事务化创建租户 + 默认部门 + 管理员 + 角色绑定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    /** 默认管理员角色编码（V43 已 seed） */
    private static final String DEFAULT_ADMIN_ROLE_CODE = "tenant_admin";

    private final SysUserService userService;
    private final SysDeptService deptService;
    private final SysRoleService roleService;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantRegisterResult registerTenant(TenantRegisterRequest request) {
        validateRequest(request);

        // 1. 用户名/手机号唯一
        if (userService.getByUsername(request.getAdminUsername()) != null) {
            throw new BusinessException("管理员用户名已存在");
        }
        if (StringUtils.hasText(request.getAdminPhone())) {
            SysUser exist = userService.getOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, request.getAdminPhone()));
            if (exist != null) {
                throw new BusinessException("该手机号已被使用");
            }
        }

        // 2. 创建默认部门（顶级，parent_id=0）
        SysDept dept = new SysDept();
        dept.setParentId(0L);
        dept.setAncestors("0");
        dept.setDeptName(request.getTenantName());
        dept.setSort(0);
        dept.setStatus(1);
        deptService.save(dept);

        // 3. 创建管理员用户
        SysUser admin = new SysUser();
        admin.setUsername(request.getAdminUsername());
        admin.setPassword(BCrypt.hashpw(request.getAdminPassword()));
        admin.setNickname(StringUtils.hasText(request.getAdminNickname())
                ? request.getAdminNickname()
                : request.getAdminUsername());
        admin.setPhone(request.getAdminPhone());
        admin.setEmail(request.getAdminEmail());
        admin.setGender(0);
        admin.setStatus(1);
        admin.setUserType("admin");
        admin.setIsQuit(0);
        admin.setDeptId(dept.getId());
        userService.save(admin);

        // 4. 绑定默认管理员角色（V43 seed 中的 tenant_admin）
        SysRole adminRole = roleService.getByCode(DEFAULT_ADMIN_ROLE_CODE);
        Long roleId = adminRole != null ? adminRole.getId() : null;
        if (roleId != null) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        } else {
            log.warn("未找到 tenant_admin 角色 seed，请检查 V43 migration 是否生效");
        }

        // 5. 创建租户记录
        Tenant tenant = new Tenant();
        tenant.setName(request.getTenantName());
        tenant.setCode(generateTenantCode());
        tenant.setIndustry(request.getIndustry());
        tenant.setScale(request.getScale());
        tenant.setAdminUserId(admin.getId());
        tenant.setDefaultDeptId(dept.getId());
        tenant.setStatus(1);
        save(tenant);

        log.info("企业注册成功：tenantId={}, adminUserId={}, deptId={}", tenant.getId(), admin.getId(), dept.getId());

        TenantRegisterResult result = new TenantRegisterResult();
        result.setTenantId(tenant.getId());
        result.setTenantCode(tenant.getCode());
        result.setAdminUserId(admin.getId());
        result.setDefaultDeptId(dept.getId());
        result.setDefaultRoleId(roleId);
        return result;
    }

    @Override
    public Tenant getByCode(String code) {
        return getOne(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code));
    }

    private void validateRequest(TenantRegisterRequest request) {
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getTenantName())) {
            throw new BusinessException("企业名称不能为空");
        }
        if (!StringUtils.hasText(request.getAdminUsername())
                || !request.getAdminUsername().matches("^[a-zA-Z0-9_]{4,20}$")) {
            throw new BusinessException("管理员用户名只能包含字母、数字、下划线，长度4-20位");
        }
        if (!StringUtils.hasText(request.getAdminPassword())
                || request.getAdminPassword().length() < 6) {
            throw new BusinessException("管理员密码不能少于 6 位");
        }
    }

    /**
     * 生成租户编码：t + 短 UUID 前 12 位（小写）
     */
    private String generateTenantCode() {
        String code;
        int retry = 0;
        do {
            code = "t" + IdUtil.simpleUUID().substring(0, 11).toLowerCase();
            retry++;
            if (retry > 5) {
                throw new BusinessException("生成租户编码失败，请重试");
            }
        } while (getByCode(code) != null);
        return code;
    }
}
