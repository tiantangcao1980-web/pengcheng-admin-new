package com.pengcheng.admin.controller.meeting;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.meeting.attendance.entity.MeetingAttendance;
import com.pengcheng.system.meeting.attendance.service.MeetingAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议签到接口（Phase 4 J5）
 * 路径：/admin/meeting/attendance
 */
@RestController
@RequestMapping("/admin/meeting/attendance")
@RequiredArgsConstructor
public class MeetingAttendanceController {

    private final MeetingAttendanceService meetingAttendanceService;

    /**
     * 签到
     * 请求体携带 bookingId 和 signType（可选，默认 QRCODE）
     */
    @PostMapping("/sign")
    public Result<MeetingAttendance> sign(@RequestBody MeetingAttendance attendance) {
        attendance.setUserId(StpUtil.getLoginIdAsLong());
        return Result.ok(meetingAttendanceService.sign(attendance));
    }

    /**
     * 查询某次预订的签到列表
     */
    @GetMapping("/booking/{bookingId}")
    public Result<List<MeetingAttendance>> listByBooking(@PathVariable Long bookingId) {
        return Result.ok(meetingAttendanceService.listByBooking(bookingId));
    }
}
