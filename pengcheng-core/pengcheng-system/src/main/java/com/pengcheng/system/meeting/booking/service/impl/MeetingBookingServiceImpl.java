package com.pengcheng.system.meeting.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.meeting.booking.entity.MeetingBooking;
import com.pengcheng.system.meeting.booking.mapper.MeetingBookingMapper;
import com.pengcheng.system.meeting.booking.service.MeetingBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议预订服务实现（Phase 4 J5）
 *
 * <p>时间冲突判定 SQL：
 * {@code WHERE room_id=? AND status<>3 AND NOT (end_time<=? OR start_time>=?)}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingBookingServiceImpl implements MeetingBookingService {

    private final MeetingBookingMapper meetingBookingMapper;

    @Override
    @Transactional
    public MeetingBooking book(MeetingBooking booking) {
        // 有会议室时，检测时间冲突
        if (booking.getRoomId() != null) {
            int conflict = meetingBookingMapper.countConflict(
                    booking.getRoomId(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    null
            );
            if (conflict > 0) {
                throw new IllegalStateException(
                        "会议室 [" + booking.getRoomId() + "] 在该时段已有预订，请选择其他时间或会议室");
            }
        }
        booking.setStatus(0);
        meetingBookingMapper.insert(booking);
        return booking;
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        MeetingBooking booking = meetingBookingMapper.selectById(id);
        if (booking == null) {
            return;
        }
        booking.setStatus(3);
        meetingBookingMapper.updateById(booking);
    }

    @Override
    public List<MeetingBooking> listByUser(Long organizerId) {
        return meetingBookingMapper.selectList(
                new LambdaQueryWrapper<MeetingBooking>()
                        .eq(MeetingBooking::getOrganizerId, organizerId)
                        .orderByDesc(MeetingBooking::getStartTime)
        );
    }

    @Override
    public List<MeetingBooking> listByRoom(Long roomId, LocalDateTime start, LocalDateTime end) {
        return meetingBookingMapper.listByRoomAndTimeRange(roomId, start, end);
    }

    @Override
    public MeetingBooking getById(Long id) {
        return meetingBookingMapper.selectById(id);
    }
}
