package com.pengcheng.integration.wecom;

import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import com.pengcheng.integration.spi.dto.ImUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WecomAuthServiceImpl 单元测试")
class WecomAuthServiceImplTest {

    @Mock
    private IntegrationProviderConfigMapper configMapper;

    @Mock
    private WecomTokenCache tokenCache;

    @Mock
    private WecomHttpClient httpClient;

    @InjectMocks
    private WecomAuthServiceImpl authService;

    private IntegrationProviderConfig mockConfig;

    @BeforeEach
    void setUp() {
        mockConfig = new IntegrationProviderConfig();
        mockConfig.setTenantId(1L);
        mockConfig.setProvider("wecom");
        mockConfig.setCorpId("wx_corp_001");
        mockConfig.setAgentId("1000001");
        mockConfig.setSecretRef("test_secret");
    }

    @Test
    @DisplayName("buildAuthorizeUrl - 正常拼接授权 URL")
    void buildAuthorizeUrl_shouldReturnCorrectUrl() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);

        String url = authService.buildAuthorizeUrl(1L,
                "https://example.com/callback", "random_state");

        assertThat(url).contains("open.work.weixin.qq.com/wwopen/sso/qrConnect");
        assertThat(url).contains("appid=wx_corp_001");
        assertThat(url).contains("agentid=1000001");
        assertThat(url).contains("state=random_state");
        assertThat(url).contains("redirect_uri=");
    }

    @Test
    @DisplayName("handleCallback - 正常流程返回 ImUserInfo")
    void handleCallback_shouldReturnImUserInfo() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken("wx_corp_001", "test_secret")).thenReturn("fake_token");

        // getuserinfo 返回
        Map<String, Object> userInfoResp = Map.of(
                "errcode", 0,
                "UserId", "zhangsan");
        // user/get 返回
        Map<String, Object> userDetailResp = Map.of(
                "errcode", 0,
                "name", "张三",
                "mobile", "13800138000",
                "email", "zhangsan@test.com",
                "avatar", "https://avatar.url/1",
                "department", List.of(1, 2));

        when(httpClient.get(contains("getuserinfo"))).thenReturn(userInfoResp);
        when(httpClient.get(contains("user/get?"))).thenReturn(userDetailResp);

        ImUserInfo info = authService.handleCallback(1L, "auth_code_001");

        assertThat(info.getProvider()).isEqualTo("wecom");
        assertThat(info.getExternalId()).isEqualTo("zhangsan");
        assertThat(info.getName()).isEqualTo("张三");
        assertThat(info.getMobile()).isEqualTo("13800138000");
        assertThat(info.getExternalDeptIds()).containsExactly("1", "2");
    }

    @Test
    @DisplayName("handleCallback - token 缓存命中后不重复获取")
    void handleCallback_tokenCacheHit() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("cached_token");
        when(httpClient.get(contains("getuserinfo"))).thenReturn(Map.of("errcode", 0, "UserId", "user1"));
        when(httpClient.get(contains("user/get?"))).thenReturn(Map.of("errcode", 0, "name", "User1",
                "mobile", "", "email", "", "avatar", ""));

        authService.handleCallback(1L, "code1");
        authService.handleCallback(1L, "code2");

        // token 缓存应只由 WecomTokenCache 管理，调用次数由 cache 内部控制
        verify(tokenCache, times(2)).getToken("wx_corp_001", "test_secret");
    }

    @Test
    @DisplayName("handleCallback - HTTP 调用失败时抛出 WecomApiException")
    void handleCallback_httpFailureShouldThrow() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("fake_token");
        when(httpClient.get(contains("getuserinfo")))
                .thenThrow(new WecomApiException(40014, "invalid access_token"));

        assertThatThrownBy(() -> authService.handleCallback(1L, "bad_code"))
                .isInstanceOf(WecomApiException.class)
                .hasMessageContaining("40014");
    }
}
