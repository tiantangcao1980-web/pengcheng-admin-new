package com.pengcheng.system.meeting.booking;

import com.pengcheng.system.meeting.booking.entity.MeetingBooking;
import com.pengcheng.system.meeting.booking.mapper.MeetingBookingMapper;
import com.pengcheng.system.meeting.booking.service.impl.MeetingBookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MeetingBookingServiceImpl 单元测试（Phase 4 J5）
 * 5 个用例：创建/时间冲突拒绝/取消/列表/无 room 也可创建
 */
@ExtendWith(MockitoExtension.class)
class MeetingBookingServiceImplTest {

    @Mock
    private MeetingBookingMapper meetingBookingMapper;

    @InjectMocks
    private MeetingBookingServiceImpl meetingBookingService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 26, 10, 0);

    // ---- 用例 1：正常创建预订 ----
    @Test
    void book_shouldInsertAndReturnBooking_whenNoConflict() {
        when(meetingBookingMapper.countConflict(eq(1L), any(), any(), isNull())).thenReturn(0);

        MeetingBooking booking = buildBooking(1L);
        MeetingBooking result = meetingBookingService.book(booking);

        verify(meetingBookingMapper).insert(booking);
        assertEquals(0, result.getStatus());
        assertEquals(1L, result.getRoomId());
    }

    // ---- 用例 2：时间冲突拒绝创建 ----
    @Test
    void book_shouldThrowIllegalState_whenConflictDetected() {
        when(meetingBookingMapper.countConflict(eq(2L), any(), any(), isNull())).thenReturn(1);

        MeetingBooking booking = buildBooking(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> meetingBookingService.book(booking));
        assertTrue(ex.getMessage().contains("已有预订"));
        verify(meetingBookingMapper, never()).insert(any());
    }

    // ---- 用例 3：取消预订（status → 3）----
    @Test
    void cancel_shouldSetStatusCancelled() {
        MeetingBooking booking = buildBooking(1L);
        booking.setId(10L);
        booking.setStatus(0);
        when(meetingBookingMapper.selectById(10L)).thenReturn(booking);

        meetingBookingService.cancel(10L);

        ArgumentCaptor<MeetingBooking> captor = ArgumentCaptor.forClass(MeetingBooking.class);
        verify(meetingBookingMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getStatus());
    }

    // ---- 用例 4：按用户查询列表 ----
    @Test
    void listByUser_shouldDelegateToMapper() {
        MeetingBooking b1 = buildBooking(1L);
        b1.setOrganizerId(5L);
        when(meetingBookingMapper.selectList(any())).thenReturn(List.of(b1));

        List<MeetingBooking> result = meetingBookingService.listByUser(5L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getOrganizerId());
    }

    // ---- 用例 5：无会议室（纯线上会议）也可创建 ----
    @Test
    void book_shouldSucceed_whenRoomIdIsNull() {
        MeetingBooking booking = new MeetingBooking();
        booking.setTitle("线上会议");
        booking.setOrganizerId(1L);
        booking.setRoomId(null);
        booking.setStartTime(NOW);
        booking.setEndTime(NOW.plusHours(1));

        MeetingBooking result = meetingBookingService.book(booking);

        verify(meetingBookingMapper, never()).countConflict(any(), any(), any(), any());
        verify(meetingBookingMapper).insert(booking);
        assertEquals(0, result.getStatus());
    }

    // ---- 工具方法 ----
    private MeetingBooking buildBooking(Long roomId) {
        MeetingBooking b = new MeetingBooking();
        b.setTitle("测试会议");
        b.setOrganizerId(1L);
        b.setRoomId(roomId);
        b.setStartTime(NOW);
        b.setEndTime(NOW.plusHours(2));
        return b;
    }
}
