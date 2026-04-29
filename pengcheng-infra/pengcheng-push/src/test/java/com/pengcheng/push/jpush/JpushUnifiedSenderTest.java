package com.pengcheng.push.jpush;

import com.pengcheng.push.unified.PushChannelLog;
import com.pengcheng.push.unified.PushChannelLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JpushUnifiedSender 单元测试
 *
 * <p>覆盖：成功 / token 失效（errcode=3002）/ 网络失败 / 配置不完整（NoOp 降级）
 * <p>通过包可见构造函数注入 mock {@link JpushHttpClient}，无需 static mock。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpushUnifiedSender 测试")
class JpushUnifiedSenderTest {

    @Mock
    private PushChannelLogMapper logMapper;

    @Mock
    private JpushHttpClient httpClient;

    private JpushUnifiedSender sender;

    @BeforeEach
    void setUp() {
        sender = new JpushUnifiedSender("test-app-key", "test-master-secret", logMapper, httpClient);
    }

    // ======================== T1：成功推送 ========================

    @Test
    @DisplayName("T1: JPush API 返回 msg_id → 返回 true，不写审计日志")
    void sendToUser_success_returnsTrue_noAuditLog() {
        when(httpClient.post(anyString(), anyString(), anyString()))
                .thenReturn("{\"msg_id\":123456,\"sendno\":\"test\"}");

        boolean result = sender.sendToUser("user-001", "测试标题", "测试内容",
                Map.of("source", "e2e-test"));

        assertThat(result).isTrue();
        verify(logMapper, never()).insert(any());
    }

    // ======================== T2：Token/AppKey 失效 ========================

    @Test
    @DisplayName("T2: JPush 返回 error.code=3002（appKey 失效）→ 返回 false，写 FAIL 审计日志")
    void sendToUser_tokenExpired_returnsFalse_writesAuditFail() {
        when(httpClient.post(anyString(), anyString(), anyString()))
                .thenReturn("{\"error\":{\"code\":3002,\"message\":\"appkey invalid\"}}");

        boolean result = sender.sendToUser("user-002", "标题", "内容", null);

        assertThat(result).isFalse();

        ArgumentCaptor<PushChannelLog> captor = ArgumentCaptor.forClass(PushChannelLog.class);
        verify(logMapper, times(1)).insert(captor.capture());
        PushChannelLog log = captor.getValue();
        assertThat(log.getAuditStatus()).isEqualTo(PushChannelLog.STATUS_FAIL);
        assertThat(log.getChannel()).isEqualTo("jpush");
        assertThat(log.getTarget()).isEqualTo("user-002");
        assertThat(log.getFailReason()).contains("3002");
        assertThat(log.getCreateTime()).isNotNull();
    }

    // ======================== T3：网络失败 ========================

    @Test
    @DisplayName("T3: HTTP 客户端抛出 JpushNetworkException → 返回 false，写 FAIL 审计日志含错误信息")
    void sendToUser_networkFailure_returnsFalse_writesAuditFail() {
        when(httpClient.post(anyString(), anyString(), anyString()))
                .thenThrow(new JpushNetworkException(
                        "Connection refused: api.jpush.cn", new RuntimeException("timeout")));

        boolean result = sender.sendToUser("user-003", "标题", "内容", null);

        assertThat(result).isFalse();

        ArgumentCaptor<PushChannelLog> captor = ArgumentCaptor.forClass(PushChannelLog.class);
        verify(logMapper, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getAuditStatus()).isEqualTo(PushChannelLog.STATUS_FAIL);
        assertThat(captor.getValue().getFailReason()).containsIgnoringCase("Connection refused");
    }

    // ======================== T4：配置不完整 ========================

    @Test
    @DisplayName("T4: appKey 为空 → 立即返回 false，写 FAIL 审计日志，不调用 httpClient")
    void sendToUser_missingConfig_returnsFalse_noHttpCall() {
        JpushUnifiedSender unconfiguredSender =
                new JpushUnifiedSender("", "", logMapper, httpClient);

        boolean result = unconfiguredSender.sendToUser("user-004", "标题", "内容", null);

        assertThat(result).isFalse();
        // 不应发起 HTTP 请求
        verifyNoInteractions(httpClient);
        // 应写 FAIL 审计日志
        ArgumentCaptor<PushChannelLog> captor = ArgumentCaptor.forClass(PushChannelLog.class);
        verify(logMapper, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getAuditStatus()).isEqualTo(PushChannelLog.STATUS_FAIL);
    }

    // ======================== T5：API 返回未知响应 ========================

    @Test
    @DisplayName("T5: API 返回既无 msg_id 也无 error 的未知响应 → 返回 false，写 FAIL 审计日志")
    void sendToUser_unknownResponse_returnsFalse() {
        when(httpClient.post(anyString(), anyString(), anyString()))
                .thenReturn("{\"status\":\"ok\"}"); // 无 msg_id

        boolean result = sender.sendToUser("user-005", "标题", "内容", null);

        assertThat(result).isFalse();
        verify(logMapper, times(1)).insert(any());
    }

    // ======================== T6：isNoOp / channelCode ========================

    @Test
    @DisplayName("T6: isNoOp=false，channelCode=jpush")
    void channelMeta() {
        assertThat(sender.isNoOp()).isFalse();
        assertThat(sender.channelCode()).isEqualTo("jpush");
    }
}
