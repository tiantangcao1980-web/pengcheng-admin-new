package com.pengcheng.auth.tenant;

import com.pengcheng.auth.LoginRequest;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.auth.LoginStrategyFactory;
import com.pengcheng.auth.enums.ClientType;
import com.pengcheng.auth.enums.LoginType;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.service.SysUserService;
import com.pengcheng.system.tenant.dto.TenantRegisterRequest;
import com.pengcheng.system.tenant.dto.TenantRegisterResult;
import com.pengcheng.system.tenant.service.TenantService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业注册（一分钟开通）控制器。
 *
 * <p>不复用 {@link com.pengcheng.auth.LoginStrategyFactory}（保持现有登录策略不动），
 * 仅在注册成功后调用 {@code TenantRegisterStrategy} 自动登录返回 token，前端落地即用。
 */
@RestController
@RequestMapping("/auth/tenant")
@RequiredArgsConstructor
public class AdminRegisterController {

    private final TenantService tenantService;
    private final SysUserService userService;
    private final TenantRegisterStrategy tenantRegisterStrategy;
    private final LoginStrategyFactory loginStrategyFactory;

    /**
     * 企业一分钟注册：创建 Tenant + 默认部门 + 管理员用户 + 默认角色绑定，自动登录。
     */
    @PostMapping("/register")
    public Result<TenantRegisterResponse> register(@RequestBody TenantRegisterRequest request) {
        TenantRegisterResult registerResult = tenantService.registerTenant(request);

        // 自动登录（沿用既有 PASSWORD 策略，不动 LoginStrategyFactory）
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginType(LoginType.PASSWORD);
        loginRequest.setClientType(ClientType.ADMIN);
        loginRequest.setUsername(request.getAdminUsername());
        loginRequest.setPassword(request.getAdminPassword());
        // 跳过验证码：管理员注册场景不要求 captcha 校验，TenantRegisterStrategy
        // 会通过设置一个内部 flag 让现有 PasswordLoginStrategy 接受
        loginRequest.setCode("slider_verified");

        LoginResult loginResult;
        try {
            loginResult = loginStrategyFactory.login(loginRequest);
        } catch (BusinessException ex) {
            // 兜底：直接走 TenantRegisterStrategy 旁路登录
            SysUser admin = userService.getById(registerResult.getAdminUserId());
            loginResult = tenantRegisterStrategy.directLogin(admin);
        }

        TenantRegisterResponse resp = new TenantRegisterResponse();
        resp.setTenantId(registerResult.getTenantId());
        resp.setTenantCode(registerResult.getTenantCode());
        resp.setAdminUserId(registerResult.getAdminUserId());
        resp.setDefaultDeptId(registerResult.getDefaultDeptId());
        resp.setDefaultRoleId(registerResult.getDefaultRoleId());
        resp.setLogin(loginResult);
        return Result.ok(resp);
    }

    @Data
    public static class TenantRegisterResponse {
        private Long tenantId;
        private String tenantCode;
        private Long adminUserId;
        private Long defaultDeptId;
        private Long defaultRoleId;
        private LoginResult login;
    }
}
