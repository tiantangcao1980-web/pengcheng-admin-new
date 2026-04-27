package com.pengcheng.system.meeting.attendance;

import com.pengcheng.system.meeting.attendance.entity.MeetingAttendance;
import com.pengcheng.system.meeting.attendance.mapper.MeetingAttendanceMapper;
import com.pengcheng.system.meeting.attendance.service.impl.MeetingAttendanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MeetingAttendanceServiceImpl 单元测试（Phase 4 J5）
 * 3 个用例：签到/重复签到唯一约束/列表
 */
@ExtendWith(MockitoExtension.class)
class MeetingAttendanceServiceImplTest {

    @Mock
    private MeetingAttendanceMapper meetingAttendanceMapper;

    @InjectMocks
    private MeetingAttendanceServiceImpl meetingAttendanceService;

    // ---- 用例 1：正常签到 ----
    @Test
    void sign_shouldInsertAndReturnAttendance() {
        MeetingAttendance attendance = buildAttendance(100L, 1L);
        attendance.setSignType(null); // 测试默认值填充

        MeetingAttendance result = meetingAttendanceService.sign(attendance);

        verify(meetingAttendanceMapper).insert(attendance);
        assertEquals("QRCODE", result.getSignType());
        assertNotNull(result.getSignTime());
    }

    // ---- 用例 2：重复签到抛 IllegalStateException ----
    @Test
    void sign_shouldThrowIllegalState_whenDuplicateKey() {
        MeetingAttendance attendance = buildAttendance(100L, 2L);
        doThrow(new DuplicateKeyException("uk_booking_user"))
                .when(meetingAttendanceMapper).insert(any());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> meetingAttendanceService.sign(attendance));
        assertTrue(ex.getMessage().contains("已签到"));
    }

    // ---- 用例 3：查询签到列表 ----
    @Test
    void listByBooking_shouldReturnAttendanceList() {
        MeetingAttendance a1 = buildAttendance(200L, 10L);
        MeetingAttendance a2 = buildAttendance(200L, 11L);
        when(meetingAttendanceMapper.selectList(any())).thenReturn(List.of(a1, a2));

        List<MeetingAttendance> result = meetingAttendanceService.listByBooking(200L);

        assertEquals(2, result.size());
        assertEquals(200L, result.get(0).getBookingId());
    }

    // ---- 工具方法 ----
    private MeetingAttendance buildAttendance(Long bookingId, Long userId) {
        MeetingAttendance a = new MeetingAttendance();
        a.setBookingId(bookingId);
        a.setUserId(userId);
        a.setSignTime(LocalDateTime.now());
        a.setSignType("QRCODE");
        return a;
    }
}
