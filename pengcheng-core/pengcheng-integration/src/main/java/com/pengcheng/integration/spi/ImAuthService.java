package com.pengcheng.integration.spi;

import com.pengcheng.integration.spi.dto.ImUserInfo;

/**
 * IM SSO 认证服务 SPI。
 * <p>
 * 负责构造 OAuth 授权 URL 与解析回调 code 拿到外部用户信息。
 */
public interface ImAuthService {

    /**
     * 构造 IM 平台 OAuth 授权 URL。
     *
     * @param tenantId    租户 ID（用于加载对应 provider 配置）
     * @param redirectUri 回调地址（应与 IM 平台后台配置一致）
     * @param state       防 CSRF 随机串，回调后原样返回
     * @return 完整授权页 URL
     */
    String buildAuthorizeUrl(Long tenantId, String redirectUri, String state);

    /**
     * 处理 OAuth 回调，用 code 换取外部用户信息。
     *
     * @param tenantId 租户 ID
     * @param code     IM 平台回传的 authorization_code
     * @return 外部用户信息（含 externalId、姓名、手机等）
     */
    ImUserInfo handleCallback(Long tenantId, String code);
}
