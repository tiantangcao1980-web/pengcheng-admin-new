package com.pengcheng.system.meeting.booking.service;

import com.pengcheng.system.meeting.booking.entity.MeetingBooking;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议预订服务接口（Phase 4 J5）
 */
public interface MeetingBookingService {

    /**
     * 创建预订；若会议室时间冲突抛 IllegalStateException
     */
    MeetingBooking book(MeetingBooking booking);

    /**
     * 取消预订（status=3）
     */
    void cancel(Long id);

    /** 根据组织者查询预订列表，按开始时间倒序 */
    List<MeetingBooking> listByUser(Long organizerId);

    /**
     * 按会议室 + 时段范围查询（非取消状态，有时间交集的预订）
     */
    List<MeetingBooking> listByRoom(Long roomId, LocalDateTime start, LocalDateTime end);

    /** 根据 id 查询 */
    MeetingBooking getById(Long id);
}
