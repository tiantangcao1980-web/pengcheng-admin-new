package com.pengcheng.admin.controller.meeting;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.meeting.booking.entity.MeetingBooking;
import com.pengcheng.system.meeting.booking.service.MeetingBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议预订接口（Phase 4 J5）
 * 路径：/admin/meeting/bookings
 */
@RestController
@RequestMapping("/admin/meeting/bookings")
@RequiredArgsConstructor
public class MeetingBookingController {

    private final MeetingBookingService meetingBookingService;

    /**
     * 新建预订
     */
    @PostMapping
    public Result<MeetingBooking> book(@RequestBody MeetingBooking booking) {
        booking.setOrganizerId(StpUtil.getLoginIdAsLong());
        return Result.ok(meetingBookingService.book(booking));
    }

    /**
     * 取消预订
     */
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        meetingBookingService.cancel(id);
        return Result.ok();
    }

    /**
     * 查询当前用户的预订列表
     */
    @GetMapping("/by-user")
    public Result<List<MeetingBooking>> listByUser() {
        return Result.ok(meetingBookingService.listByUser(StpUtil.getLoginIdAsLong()));
    }

    /**
     * 按会议室 + 时段查询占用情况
     */
    @GetMapping("/by-room")
    public Result<List<MeetingBooking>> listByRoom(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Result.ok(meetingBookingService.listByRoom(roomId, start, end));
    }

    /**
     * 查询预订详情
     */
    @GetMapping("/{id}")
    public Result<MeetingBooking> getById(@PathVariable Long id) {
        return Result.ok(meetingBookingService.getById(id));
    }
}
