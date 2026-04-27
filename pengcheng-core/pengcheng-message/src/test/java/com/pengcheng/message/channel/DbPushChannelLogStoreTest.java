package com.pengcheng.message.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F4 单测：DbPushChannelLogStore 应当：
 * 1) 调 mapper.insert 落库；
 * 2) 自动补 createTime；
 * 3) null 入参直接返回不报错；
 * 4) mapper 抛异常时吞掉 + WARN（不传染主链路）。
 */
class DbPushChannelLogStoreTest {

    private PushChannelLogMapper mapper;
    private DbPushChannelLogStore store;

    @BeforeEach
    void setUp() {
        mapper = mock(PushChannelLogMapper.class);
        store = new DbPushChannelLogStore(mapper);
    }

    @Test
    void save_should_insert_into_mapper() {
        PushChannelLog log = PushChannelLog.builder()
                .userId(100L)
                .channel("appPush")
                .bizType("approval")
                .bizId(42L)
                .success(1)
                .build();

        store.save(log);

        ArgumentCaptor<PushChannelLog> captor = ArgumentCaptor.forClass(PushChannelLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getUserId());
        assertEquals("appPush", captor.getValue().getChannel());
        assertNotNull(captor.getValue().getCreateTime(), "createTime 应当被自动补齐");
    }

    @Test
    void save_should_keep_existing_create_time() {
        LocalDateTime existing = LocalDateTime.of(2026, 4, 1, 12, 0);
        PushChannelLog log = PushChannelLog.builder()
                .userId(1L)
                .channel("webInbox")
                .createTime(existing)
                .success(1)
                .build();

        store.save(log);

        ArgumentCaptor<PushChannelLog> captor = ArgumentCaptor.forClass(PushChannelLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals(existing, captor.getValue().getCreateTime());
    }

    @Test
    void save_should_swallow_null_input() {
        assertDoesNotThrow(() -> store.save(null));
        verify(mapper, never()).insert(null);
    }

    @Test
    void save_should_swallow_mapper_exception_to_protect_push_main_flow() {
        PushChannelLog log = PushChannelLog.builder()
                .userId(2L)
                .channel("mpSubscribe")
                .success(0)
                .reason("token expired")
                .build();
        when(mapper.insert(log)).thenThrow(new RuntimeException("DB connection lost"));

        // 关键断言：异常被吞掉，不会传染调用方（推送主链路）
        assertDoesNotThrow(() -> store.save(log));
    }
}
