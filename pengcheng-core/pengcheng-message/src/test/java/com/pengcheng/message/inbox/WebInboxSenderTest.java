package com.pengcheng.message.inbox;

import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * WebInboxSender 单元测试
 *
 * <p>覆盖：成功（含 WebSocket 推送）/ NotificationService 异常 / bridge=null（离线兜底）
 * / bridge 推送失败（不影响落库结果）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebInboxSender 测试")
class WebInboxSenderTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private InboxWebSocketBridge webSocketBridge;

    private WebInboxSender sender;

    @BeforeEach
    void setUp() {
        sender = new WebInboxSender(notificationService, webSocketBridge);
    }

    // ======================== T1：成功落库 + WebSocket 推送 ========================

    @Test
    @DisplayName("T1: createNotification 成功 → 返回 true，调用 WebSocket bridge")
    void send_success_returnsTrueAndCallsBridge() {
        // notificationService.createNotification 默认不抛异常（void 方法）

        boolean result = sender.send(1001L, "审批通过", "您的请假申请已审批通过", "approval", 88L);

        assertThat(result).isTrue();

        // 验证落库调用
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(1)).createNotification(captor.capture());
        Notification n = captor.getValue();
        assertThat(n.getUserId()).isEqualTo(1001L);
        assertThat(n.getTitle()).isEqualTo("审批通过");
        assertThat(n.getContent()).isEqualTo("您的请假申请已审批通过");
        assertThat(n.getBizType()).isEqualTo("approval");
        assertThat(n.getBizId()).isEqualTo(88L);

        // 验证 WebSocket 推送
        verify(webSocketBridge, times(1))
                .sendNoticeIfOnline(eq(1001L), eq("审批通过"), eq("您的请假申请已审批通过"));
    }

    // ======================== T2：NotificationService 异常 ========================

    @Test
    @DisplayName("T2: createNotification 抛出异常 → 返回 false，不调用 WebSocket bridge")
    void send_notificationServiceThrows_returnsFalse_noBridge() {
        doThrow(new RuntimeException("DB 连接超时"))
                .when(notificationService).createNotification(any());

        boolean result = sender.send(1002L, "标题", "内容", "customer", 99L);

        assertThat(result).isFalse();
        verifyNoInteractions(webSocketBridge);
    }

    // ======================== T3：bridge 为 null（无 WebSocket 环境）========================

    @Test
    @DisplayName("T3: bridge=null → 落库成功返回 true，跳过 WebSocket 推送不报错")
    void send_nullBridge_returnsTrueWithoutWebSocket() {
        WebInboxSender senderWithoutBridge = new WebInboxSender(notificationService, null);

        boolean result = senderWithoutBridge.send(1003L, "新审批", "待处理审批", "leave", 77L);

        assertThat(result).isTrue();
        verify(notificationService, times(1)).createNotification(any());
    }

    // ======================== T4：WebSocket bridge 推送失败不影响落库结果 ========================

    @Test
    @DisplayName("T4: bridge.sendNoticeIfOnline 抛出异常 → 落库已完成，返回 true（fire-and-forget）")
    void send_bridgeThrows_stillReturnsTrueAfterPersist() {
        doThrow(new RuntimeException("WebSocket session closed"))
                .when(webSocketBridge).sendNoticeIfOnline(anyLong(), anyString(), anyString());

        boolean result = sender.send(1004L, "标题", "内容", "approval", 55L);

        // 落库成功，WebSocket 推送失败不影响返回值
        assertThat(result).isTrue();
        verify(notificationService, times(1)).createNotification(any());
    }

    // ======================== T5：isNoOp / channelCode ========================

    @Test
    @DisplayName("T5: isNoOp=false，channelCode=inbox")
    void channelMeta() {
        assertThat(sender.isNoOp()).isFalse();
        assertThat(sender.channelCode()).isEqualTo("inbox");
    }
}
