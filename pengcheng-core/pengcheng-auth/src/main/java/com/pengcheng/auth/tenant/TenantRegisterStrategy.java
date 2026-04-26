package com.pengcheng.auth.tenant;

import com.pengcheng.auth.LoginHelper;
import com.pengcheng.auth.LoginResult;
import com.pengcheng.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企业注册旁路登录策略。
 *
 * <p>注：刻意不实现 {@link com.pengcheng.auth.LoginStrategy} 接口，避免被
 * {@code LoginStrategyFactory} 自动收集——按红线"不动现有 LoginStrategyFactory"。
 * 这是一个独立的"注册即登录"工具，仅供 {@link AdminRegisterController} 兜底调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantRegisterStrategy {

    private final LoginHelper loginHelper;

    /**
     * 注册完成后直接登录（绕过密码校验，因刚刚由我方落库）。
     */
    public LoginResult directLogin(SysUser admin) {
        log.info("[TenantRegister] direct login: userId={}", admin.getId());
        return loginHelper.doLogin(admin);
    }
}
