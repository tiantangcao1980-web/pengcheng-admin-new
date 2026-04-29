package com.pengcheng.system.meeting.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.meeting.attendance.entity.MeetingAttendance;
import com.pengcheng.system.meeting.attendance.mapper.MeetingAttendanceMapper;
import com.pengcheng.system.meeting.attendance.service.MeetingAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议签到服务实现（Phase 4 J5）
 *
 * <p>签到幂等：数据库 uk_booking_user 唯一约束；
 * 捕获 DuplicateKeyException 转为 IllegalStateException。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingAttendanceServiceImpl implements MeetingAttendanceService {

    private final MeetingAttendanceMapper meetingAttendanceMapper;

    @Override
    public MeetingAttendance sign(MeetingAttendance attendance) {
        if (attendance.getSignTime() == null) {
            attendance.setSignTime(LocalDateTime.now());
        }
        if (attendance.getSignType() == null) {
            attendance.setSignType("QRCODE");
        }
        try {
            meetingAttendanceMapper.insert(attendance);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException(
                    "用户 [" + attendance.getUserId() + "] 已签到会议 [" + attendance.getBookingId() + "]");
        }
        return attendance;
    }

    @Override
    public List<MeetingAttendance> listByBooking(Long bookingId) {
        return meetingAttendanceMapper.selectList(
                new LambdaQueryWrapper<MeetingAttendance>()
                        .eq(MeetingAttendance::getBookingId, bookingId)
                        .orderByAsc(MeetingAttendance::getSignTime)
        );
    }
}
