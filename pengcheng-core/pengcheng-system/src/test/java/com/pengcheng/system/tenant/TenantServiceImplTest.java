package com.pengcheng.system.tenant;

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
import com.pengcheng.system.tenant.service.impl.TenantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceImpl")
class TenantServiceImplTest {

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private SysUserService userService;

    @Mock
    private SysDeptService deptService;

    @Mock
    private SysRoleService roleService;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    private TenantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new TenantServiceImpl(userService, deptService, roleService, userRoleMapper));
        ReflectionTestUtils.setField(service, "baseMapper", tenantMapper);
    }

    private TenantRegisterRequest validRequest() {
        TenantRegisterRequest req = new TenantRegisterRequest();
        req.setTenantName("Pengcheng Demo");
        req.setIndustry("realty");
        req.setScale("1-50");
        req.setAdminUsername("admin_demo");
        req.setAdminPassword("Pwd@12345");
        req.setAdminNickname("Demo");
        req.setAdminPhone("13800138000");
        req.setAdminEmail("demo@example.com");
        return req;
    }

    @Test
    @DisplayName("企业注册成功路径：创建 dept + admin + role 绑定 + tenant")
    void registerTenant_success() {
        // 用户名唯一
        when(userService.getByUsername("admin_demo")).thenReturn(null);
        // 部门保存：模拟自增 ID
        doAnswer(inv -> {
            SysDept d = inv.getArgument(0);
            d.setId(101L);
            return true;
        }).when(deptService).save(any(SysDept.class));
        // 用户保存：模拟自增 ID
        doAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(202L);
            return true;
        }).when(userService).save(any(SysUser.class));
        // 角色获取
        SysRole adminRole = new SysRole();
        adminRole.setId(7L);
        adminRole.setCode("tenant_admin");
        when(roleService.getByCode("tenant_admin")).thenReturn(adminRole);
        // 租户保存：模拟自增 ID
        doAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(303L);
            return 1;
        }).when(tenantMapper).insert(any(Tenant.class));

        TenantRegisterResult result = service.registerTenant(validRequest());

        assertThat(result.getTenantId()).isEqualTo(303L);
        assertThat(result.getAdminUserId()).isEqualTo(202L);
        assertThat(result.getDefaultDeptId()).isEqualTo(101L);
        assertThat(result.getDefaultRoleId()).isEqualTo(7L);
        assertThat(result.getTenantCode()).startsWith("t").hasSize(12);

        // user_role 绑定
        ArgumentCaptor<SysUserRole> urCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(userRoleMapper).insert(urCaptor.capture());
        assertThat(urCaptor.getValue().getRoleId()).isEqualTo(7L);
        assertThat(urCaptor.getValue().getUserId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("非法用户名抛业务异常")
    void registerTenant_invalidUsername() {
        TenantRegisterRequest req = validRequest();
        req.setAdminUsername("ab"); // 太短
        assertThatThrownBy(() -> service.registerTenant(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名");
        verify(userService, never()).save(any(SysUser.class));
    }

    @Test
    @DisplayName("用户名已存在抛业务异常")
    void registerTenant_duplicateUsername() {
        TenantRegisterRequest req = validRequest();
        SysUser exist = new SysUser();
        exist.setId(1L);
        when(userService.getByUsername("admin_demo")).thenReturn(exist);
        assertThatThrownBy(() -> service.registerTenant(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
        verify(deptService, never()).save(any(SysDept.class));
    }

    @Test
    @DisplayName("缺少 tenant_admin seed 时仍创建 tenant，但跳过角色绑定")
    void registerTenant_missingDefaultRole() {
        when(userService.getByUsername("admin_demo")).thenReturn(null);
        doAnswer(inv -> {
            SysDept d = inv.getArgument(0);
            d.setId(101L);
            return true;
        }).when(deptService).save(any(SysDept.class));
        doAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(202L);
            return true;
        }).when(userService).save(any(SysUser.class));
        when(roleService.getByCode("tenant_admin")).thenReturn(null);
        doAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(303L);
            return 1;
        }).when(tenantMapper).insert(any(Tenant.class));

        TenantRegisterResult result = service.registerTenant(validRequest());

        assertThat(result.getDefaultRoleId()).isNull();
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
        verify(tenantMapper, times(1)).insert(any(Tenant.class));
    }

    @Test
    @DisplayName("密码长度过短抛异常")
    void registerTenant_shortPassword() {
        TenantRegisterRequest req = validRequest();
        req.setAdminPassword("123");
        assertThatThrownBy(() -> service.registerTenant(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码");
    }
}
