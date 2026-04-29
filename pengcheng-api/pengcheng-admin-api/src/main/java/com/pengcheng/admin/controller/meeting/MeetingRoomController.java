package com.pengcheng.admin.controller.meeting;

import com.pengcheng.common.result.Result;
import com.pengcheng.system.meeting.room.entity.MeetingRoom;
import com.pengcheng.system.meeting.room.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议室管理接口（Phase 4 J5）
 * 路径：/admin/meeting/rooms
 */
@RestController
@RequestMapping("/admin/meeting/rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    @GetMapping
    public Result<List<MeetingRoom>> list(@RequestParam(required = false, defaultValue = "true") boolean enabledOnly) {
        List<MeetingRoom> rooms = enabledOnly ? meetingRoomService.listEnabled() : meetingRoomService.listAll();
        return Result.ok(rooms);
    }

    @GetMapping("/{id}")
    public Result<MeetingRoom> getById(@PathVariable Long id) {
        return Result.ok(meetingRoomService.getById(id));
    }

    @PostMapping
    public Result<MeetingRoom> create(@RequestBody MeetingRoom room) {
        return Result.ok(meetingRoomService.create(room));
    }

    @PutMapping("/{id}")
    public Result<MeetingRoom> update(@PathVariable Long id, @RequestBody MeetingRoom room) {
        room.setId(id);
        return Result.ok(meetingRoomService.update(room));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        meetingRoomService.delete(id);
        return Result.ok();
    }
}
