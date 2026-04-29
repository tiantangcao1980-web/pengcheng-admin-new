package com.pengcheng.admin.controller.integration;

import com.pengcheng.common.result.Result;
import com.pengcheng.integration.spi.dto.ImUserInfo;
import com.pengcheng.integration.wecom.WecomImProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * IM OAuth 授权回调公开端点（无需登录态）。
 * <p>
 * GET  /integration/{provider}/authorize  → 302 跳转到 IM 平台授权页
 * GET  /integration/{provider}/callback   → 处理 code，返回 ImUserInfo（前端继续换 token）
 */
@Slf4j
@RestController
@RequestMapping("/integration")
@RequiredArgsConstructor
public class IntegrationAuthController {

    private final WecomImProvider wecomImProvider;

    /**
     * 发起 OAuth 授权跳转。
     *
     * @param provider    IM provider 标识（wecom）
     * @param tenantId    租户 ID
     * @param redirectUri 前端最终落地 URI（IM 平台回调后再次跳转）
     * @param state       防 CSRF 随机串
     */
    @GetMapping("/{provider}/authorize")
    public void authorize(@PathVariable String provider,
                          @RequestParam Long tenantId,
                          @RequestParam String redirectUri,
                          @RequestParam(required = false, defaultValue = "") String state,
                          HttpServletResponse response) throws IOException {
        String authorizeUrl = resolveAuthService(provider)
                .buildAuthorizeUrl(tenantId, redirectUri, state);
        log.info("[IntegrationAuth] redirect to authorizeUrl, provider={} tenantId={}", provider, tenantId);
        response.sendRedirect(authorizeUrl);
    }

    /**
     * 处理 IM 平台 OAuth 回调，解析 code 换取外部用户信息。
     *
     * @param provider 标识
     * @param tenantId 租户 ID
     * @param code     IM 平台回传的 authorization_code
     * @return 外部用户信息（前端继续用 externalId 换内部 token）
     */
    @GetMapping("/{provider}/callback")
    public Result<ImUserInfo> callback(@PathVariable String provider,
                                       @RequestParam Long tenantId,
                                       @RequestParam String code) {
        ImUserInfo userInfo = resolveAuthService(provider).handleCallback(tenantId, code);
        log.info("[IntegrationAuth] callback ok, provider={} externalId={}", provider, userInfo.getExternalId());
        return Result.ok(userInfo);
    }

    // ---- private ----

    private com.pengcheng.integration.spi.ImAuthService resolveAuthService(String provider) {
        if ("wecom".equals(provider)) {
            return wecomImProvider.auth();
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
}
