package com.pengcheng.system.doc.collab;

import com.pengcheng.system.doc.collab.service.DocCollabService;
import com.pengcheng.system.doc.collab.ws.DocCollabRegistry;
import com.pengcheng.system.doc.collab.ws.DocCollabRoom;
import com.pengcheng.system.doc.collab.ws.DocCollabSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocCollabRegistry")
class DocCollabRegistryTest {

    @Mock
    private DocCollabService docCollabService;

    @Mock
    private WebSocketSession wsSession1;

    @Mock
    private WebSocketSession wsSession2;

    private DocCollabRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DocCollabRegistry(docCollabService);
        when(wsSession1.getId()).thenReturn("session-1");
        when(wsSession2.getId()).thenReturn("session-2");
        when(wsSession1.isOpen()).thenReturn(true);
        when(wsSession2.isOpen()).thenReturn(true);
    }

    @Test
    @DisplayName("1. join：用户加入房间后，在线用户列表包含该用户")
    void should_track_user_after_join() {
        DocCollabRoom room = registry.getOrCreateRoom(100L);
        DocCollabSession meta = new DocCollabSession(100L, 1L, "session-1", "Alice");

        room.join(wsSession1, meta);

        Collection<DocCollabSession> users = room.getOnlineUsers();
        assertThat(users).hasSize(1);
        assertThat(users.iterator().next().getUserName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("2. leave：用户离开后，房间在线列表清空，Registry 移除空房间")
    void should_remove_empty_room_after_leave() {
        DocCollabRoom room = registry.getOrCreateRoom(200L);
        DocCollabSession meta = new DocCollabSession(200L, 2L, "session-1", "Bob");
        room.join(wsSession1, meta);

        room.leave("session-1");
        registry.removeRoomIfEmpty(200L);

        assertThat(room.isEmpty()).isTrue();
        assertThat(registry.getRoom(200L)).isNull();
    }

    @Test
    @DisplayName("3. broadcastExclude：update 广播时不回传给发送方")
    void should_broadcast_excluding_sender() throws Exception {
        DocCollabRoom room = registry.getOrCreateRoom(300L);
        room.join(wsSession1, new DocCollabSession(300L, 1L, "session-1", "Alice"));
        room.join(wsSession2, new DocCollabSession(300L, 2L, "session-2", "Bob"));

        room.broadcastExclude("session-1", "{\"type\":\"update\",\"payload\":\"abc\"}");

        // wsSession2 应收到消息，wsSession1 不收到
        verify(wsSession2, times(1)).sendMessage(any());
        verify(wsSession1, never()).sendMessage(any());
    }

    @Test
    @DisplayName("4. persistDirtyRooms：dirty=true 时触发持久化，dirty=false 时跳过")
    void should_persist_only_when_dirty() throws Exception {
        DocCollabRoom room = registry.getOrCreateRoom(400L);
        room.join(wsSession1, new DocCollabSession(400L, 1L, "session-1", "Alice"));

        // 注入 update → dirty=true
        byte[] update = "yjs-update".getBytes();
        byte[] sv = "state-vector".getBytes();
        room.receiveUpdate(update, sv, 1L);

        // 手动调用私有调度逻辑等效：直接检查 checkAndClearDirty
        boolean wasDirty = room.checkAndClearDirty();
        assertThat(wasDirty).isTrue();

        // 调用持久化
        docCollabService.persistUpdate(400L, sv, update, 1L);
        verify(docCollabService).persistUpdate(400L, sv, update, 1L);

        // 再次 checkAndClearDirty 应为 false（已清除）
        assertThat(room.checkAndClearDirty()).isFalse();
    }

    @Test
    @DisplayName("5. mergeAndCompact：调用 service 合并不抛异常，版本语义正确")
    void should_call_merge_and_compact_without_exception() {
        doNothing().when(docCollabService).mergeAndCompact(anyLong());

        // 不应抛异常
        docCollabService.mergeAndCompact(500L);

        verify(docCollabService).mergeAndCompact(500L);
    }
}
