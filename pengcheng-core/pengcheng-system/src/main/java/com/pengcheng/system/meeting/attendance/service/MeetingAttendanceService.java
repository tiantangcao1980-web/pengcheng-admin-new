package com.pengcheng.system.meeting.attendance.service;

import com.pengcheng.system.meeting.attendance.entity.MeetingAttendance;

import java.util.List;

/**
 * 会议签到服务接口（Phase 4 J5）
 */
public interface MeetingAttendanceService {

    /**
     * 签到；若已签到（uk_booking_user 唯一约束）则抛 IllegalStateException
     */
    MeetingAttendance sign(MeetingAttendance attendance);

    /**
     * 查询某次预订的所有签到记录
     */
    List<MeetingAttendance> listByBooking(Long bookingId);
}
