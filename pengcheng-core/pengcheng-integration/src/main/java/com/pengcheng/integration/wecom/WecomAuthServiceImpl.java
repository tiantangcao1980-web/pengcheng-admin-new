package com.pengcheng.integration.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import com.pengcheng.integration.spi.ImAuthService;
import com.pengcheng.integration.spi.dto.ImUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 企业微信 SSO 认证服务实现。
 * <p>
 * 授权流程：
 * 1. buildAuthorizeUrl → 跳转企业微信扫码页
 * 2. handleCallback(code) → 调 getuserinfo 拿 userId → 调 user/get 拿详情
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WecomAuthServiceImpl implements ImAuthService {

    private static final String AUTHORIZE_URL =
            "https://open.work.weixin.qq.com/wwopen/sso/qrConnect?appid=%s&agentid=%s&redirect_uri=%s&state=%s";

    private static final String GET_USER_INFO_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token=%s&code=%s";

    private static final String GET_USER_DETAIL_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=%s&userid=%s";

    private final IntegrationProviderConfigMapper configMapper;
    private final WecomTokenCache                 tokenCache;
    private final WecomHttpClient                 httpClient;

    @Override
    public String buildAuthorizeUrl(Long tenantId, String redirectUri, String state) {
        IntegrationProviderConfig cfg = loadConfig(tenantId);
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return String.format(AUTHORIZE_URL,
                cfg.getCorpId(),
                cfg.getAgentId(),
                encodedRedirect,
                state);
    }

    @Override
    public ImUserInfo handleCallback(Long tenantId, String code) {
        IntegrationProviderConfig cfg    = loadConfig(tenantId);
        String                    secret = cfg.getSecretRef(); // 实际场景由 KMS 解密；此处直接用 secretRef
        String                    token  = tokenCache.getToken(cfg.getCorpId(), secret);

        // Step 1: code → userId
        String getUserInfoUrl = String.format(GET_USER_INFO_URL, token, code);
        Map<String, Object> userInfoResp = httpClient.get(getUserInfoUrl);
        String userId = (String) userInfoResp.get("UserId");
        if (userId == null || userId.isBlank()) {
            throw new WecomApiException(-1, "UserId is blank in getuserinfo response");
        }

        // Step 2: userId → 用户详情
        String getUserUrl = String.format(GET_USER_DETAIL_URL, token, userId);
        Map<String, Object> detail = httpClient.get(getUserUrl);

        ImUserInfo info = new ImUserInfo();
        info.setProvider("wecom");
        info.setExternalId(userId);
        info.setName(String.valueOf(detail.getOrDefault("name", "")));
        info.setAvatar(String.valueOf(detail.getOrDefault("avatar", "")));
        info.setMobile(String.valueOf(detail.getOrDefault("mobile", "")));
        info.setEmail(String.valueOf(detail.getOrDefault("email", "")));

        // 部门列表（企业微信返回 Integer 列表）
        Object deptObj = detail.get("department");
        if (deptObj instanceof List<?> deptList) {
            List<String> deptIds = deptList.stream()
                    .map(Object::toString)
                    .toList();
            info.setExternalDeptIds(deptIds);
        }
        return info;
    }

    // ---- private ----

    private IntegrationProviderConfig loadConfig(Long tenantId) {
        IntegrationProviderConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<IntegrationProviderConfig>()
                        .eq(IntegrationProviderConfig::getTenantId, tenantId)
                        .eq(IntegrationProviderConfig::getProvider, "wecom"));
        if (cfg == null) {
            throw new IllegalStateException("No wecom config for tenantId=" + tenantId);
        }
        return cfg;
    }
}
