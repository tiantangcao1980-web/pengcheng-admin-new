package com.pengcheng.wechat.subscribe;

import com.pengcheng.wechat.WechatMiniProgramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WechatMpSubscribeSender 单元测试
 *
 * <p>覆盖：成功 / token 失效（errcode=42001）/ 网络失败 / 模板渲染异常（data 为空）
 * / Feature Flag 关（isConfigured=false）/ getAccessToken 抛异常
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WechatMpSubscribeSender 测试")
class WechatMpSubscribeSenderTest {

    @Mock
    private WechatMiniProgramService wechatService;

    @Mock
    private WechatHttpClient httpClient;

    private WechatMpSubscribeSender sender;

    @BeforeEach
    void setUp() {
        sender = new WechatMpSubscribeSender(wechatService, httpClient);
        // 默认配置完整
        when(wechatService.isConfigured()).thenReturn(true);
        when(wechatService.getAccessToken()).thenReturn("mock-access-token");
    }

    // ======================== T1：成功发送 ========================

    @Test
    @DisplayName("T1: 微信 API 返回 errcode=0 → 返回 true，走 token 缓存（调用 getAccessToken 一次）")
    void send_success_returnsTrue() {
        when(httpClient.postJson(anyString(), anyString()))
                .thenReturn("{\"errcode\":0,\"errmsg\":\"ok\"}");

        boolean result = sender.send("openid-001", "TPL001",
                Map.of("thing1", "审批通过", "date2", "2026-04-26"), "pages/index");

        assertThat(result).isTrue();
        // 验证确实调用了 getAccessToken（走缓存路径，V3.2 WP-S6-G 已落地）
        verify(wechatService, times(1)).getAccessToken();
    }

    // ======================== T2：Token 失效 ========================

    @Test
    @DisplayName("T2: 微信 API 返回 errcode=42001（token 过期）→ 返回 false")
    void send_tokenExpired_returnsFalse() {
        when(httpClient.postJson(anyString(), anyString()))
                .thenReturn("{\"errcode\":42001,\"errmsg\":\"access_token expired\"}");

        boolean result = sender.send("openid-002", "TPL002",
                Map.of("thing1", "内容"), null);

        assertThat(result).isFalse();
    }

    // ======================== T3：网络失败 ========================

    @Test
    @DisplayName("T3: HTTP 客户端抛出 WechatNetworkException → 返回 false")
    void send_networkFailure_returnsFalse() {
        when(httpClient.postJson(anyString(), anyString()))
                .thenThrow(new WechatNetworkException(
                        "连接超时: api.weixin.qq.com", new RuntimeException("timeout")));

        boolean result = sender.send("openid-003", "TPL003",
                Map.of("thing1", "内容"), null);

        assertThat(result).isFalse();
    }

    // ======================== T4：模板渲染异常（data 为空）========================

    @Test
    @DisplayName("T4: data 为 null → WechatTemplateRenderException，返回 false，不发起 HTTP 请求")
    void send_emptyData_returnsFalse_noHttpCall() {
        boolean result = sender.send("openid-004", "TPL004", null, null);

        assertThat(result).isFalse();
        // data 为 null 时，doSend 内部抛 TemplateRenderException，不发起 HTTP 请求
        verifyNoInteractions(httpClient);
    }

    // ======================== T5：Feature Flag 关闭（未配置）========================

    @Test
    @DisplayName("T5: wechatService.isConfigured()=false → 立即返回 false，不获取 token 也不调用 HTTP")
    void send_notConfigured_returnsFalse_noTokenFetch() {
        when(wechatService.isConfigured()).thenReturn(false);

        boolean result = sender.send("openid-005", "TPL005",
                Map.of("thing1", "内容"), null);

        assertThat(result).isFalse();
        verify(wechatService, never()).getAccessToken();
        verifyNoInteractions(httpClient);
    }

    // ======================== T6：getAccessToken 抛异常 ========================

    @Test
    @DisplayName("T6: getAccessToken 抛出运行时异常 → 返回 false，不发起 HTTP 请求")
    void send_getAccessTokenThrows_returnsFalse() {
        when(wechatService.getAccessToken())
                .thenThrow(new RuntimeException("Redis 连接失败"));

        boolean result = sender.send("openid-006", "TPL006",
                Map.of("thing1", "内容"), null);

        assertThat(result).isFalse();
        verifyNoInteractions(httpClient);
    }

    // ======================== T7：isNoOp / channelCode ========================

    @Test
    @DisplayName("T7: isNoOp=false，channelCode=wechat_subscribe")
    void channelMeta() {
        assertThat(sender.isNoOp()).isFalse();
        assertThat(sender.channelCode()).isEqualTo("wechat_subscribe");
    }
}
