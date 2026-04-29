package com.pengcheng.message.service;

import com.pengcheng.message.entity.ImConversation;
import com.pengcheng.message.event.ChatMessageEvent;
import com.pengcheng.message.mapper.ImConversationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IM 会话服务单测
 */
@DisplayName("ImConversationService — 会话索引")
class ImConversationServiceTest {

    private ImConversationMapper mapper;
    private ImConversationService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ImConversationMapper.class);
        service = new ImConversationService(mapper);
    }

    @Test
    @DisplayName("会话ID 规则：min_max 保证两端一致")
    void conversationIdRule() {
        assertThat(ImConversationService.makeConversationId(10L, 20L)).isEqualTo("10_20");
        assertThat(ImConversationService.makeConversationId(20L, 10L)).isEqualTo("10_20");
    }

    @Test
    @DisplayName("收消息事件：双方都 upsert（两次 insert）")
    void onChatMessage_insertsTwoRows() {
        when(mapper.selectOne(any())).thenReturn(null);  // 都不存在 → 走 insert

        service.onChatMessage(new ChatMessageEvent(this,
                500L, 1L, 2L, "你好", 1, null));

        // sender + receiver 各 insert 一次
        ArgumentCaptor<ImConversation> cap = ArgumentCaptor.forClass(ImConversation.class);
        verify(mapper, times(2)).insert(cap.capture());

        // 发送者侧未读 = 0
        ImConversation senderRow = cap.getAllValues().stream()
                .filter(r -> r.getOwnerId().equals(1L)).findFirst().orElseThrow();
        assertThat(senderRow.getUnreadCount()).isEqualTo(0);
        assertThat(senderRow.getPeerId()).isEqualTo(2L);
        assertThat(senderRow.getConversationId()).isEqualTo("1_2");

        // 接收者侧未读 = 1
        ImConversation receiverRow = cap.getAllValues().stream()
                .filter(r -> r.getOwnerId().equals(2L)).findFirst().orElseThrow();
        assertThat(receiverRow.getUnreadCount()).isEqualTo(1);
        assertThat(receiverRow.getLastMsgContent()).isEqualTo("你好");
    }

    @Test
    @DisplayName("收消息事件：已存在会话 → update + 接收者未读 +1")
    void onChatMessage_updatesExisting() {
        ImConversation existing = ImConversation.builder()
                .ownerId(2L).peerId(1L).conversationId("1_2")
                .unreadCount(3).build();
        existing.setId(99L);
        when(mapper.selectOne(any()))
                .thenReturn(null)       // 发送者侧不存在
                .thenReturn(existing);  // 接收者侧已存在

        service.onChatMessage(new ChatMessageEvent(this,
                501L, 1L, 2L, "再来一条", 1, null));

        verify(mapper, times(1)).insert(any());
        verify(mapper, times(1)).updateById(any(ImConversation.class));
        assertThat(existing.getUnreadCount()).isEqualTo(4);
        assertThat(existing.getLastMsgId()).isEqualTo(501L);
    }

    @Test
    @DisplayName("消息预览：业务消息类型 → [businessType]")
    void preview_businessType() {
        when(mapper.selectOne(any())).thenReturn(null);

        service.onChatMessage(new ChatMessageEvent(this,
                502L, 1L, 2L, "{json}", 7, "CARD"));

        ArgumentCaptor<ImConversation> cap = ArgumentCaptor.forClass(ImConversation.class);
        verify(mapper, times(2)).insert(cap.capture());
        assertThat(cap.getAllValues().get(0).getLastMsgContent()).isEqualTo("[CARD]");
    }

    @Test
    @DisplayName("消息预览：图片消息 → [图片]")
    void preview_imageType() {
        when(mapper.selectOne(any())).thenReturn(null);

        service.onChatMessage(new ChatMessageEvent(this,
                503L, 1L, 2L, "https://oss/x.jpg", 2, null));

        ArgumentCaptor<ImConversation> cap = ArgumentCaptor.forClass(ImConversation.class);
        verify(mapper, times(2)).insert(cap.capture());
        assertThat(cap.getAllValues().get(0).getLastMsgContent()).isEqualTo("[图片]");
    }

    @Test
    @DisplayName("clearUnread：未读数清零")
    void clearUnread_resetsToZero() {
        ImConversation existing = ImConversation.builder()
                .ownerId(2L).conversationId("1_2").unreadCount(5).build();
        existing.setId(99L);
        when(mapper.selectOne(any())).thenReturn(existing);

        service.clearUnread(2L, "1_2");

        assertThat(existing.getUnreadCount()).isEqualTo(0);
        verify(mapper).updateById(existing);
    }

    @Test
    @DisplayName("clearUnread：已是 0 时 no-op")
    void clearUnread_alreadyZero() {
        ImConversation existing = ImConversation.builder()
                .ownerId(2L).conversationId("1_2").unreadCount(0).build();
        existing.setId(99L);
        when(mapper.selectOne(any())).thenReturn(existing);

        service.clearUnread(2L, "1_2");

        verify(mapper, org.mockito.Mockito.never()).updateById(any());
    }

    @Test
    @DisplayName("事件处理异常不向外抛（幂等订阅，避免影响主流程）")
    void onChatMessage_failureSwallowed() {
        when(mapper.selectOne(any())).thenThrow(new RuntimeException("DB down"));

        // 不应抛出
        service.onChatMessage(new ChatMessageEvent(this,
                500L, 1L, 2L, "x", 1, null));
    }
}
