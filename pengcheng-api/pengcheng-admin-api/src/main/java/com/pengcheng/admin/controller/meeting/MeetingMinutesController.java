package com.pengcheng.admin.controller.meeting;

import com.pengcheng.common.result.Result;
import com.pengcheng.system.meeting.minutes.entity.MeetingActionItem;
import com.pengcheng.system.meeting.minutes.entity.MeetingMinutesAi;
import com.pengcheng.system.meeting.minutes.service.MeetingMinutesAiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 纪要接口（Phase 4 J5）
 * 路径：/admin/meeting/minutes
 */
@RestController
@RequestMapping("/admin/meeting/minutes")
@RequiredArgsConstructor
public class MeetingMinutesController {

    private final MeetingMinutesAiService meetingMinutesAiService;

    /**
     * 触发 AI 纪要处理（幂等）
     * POST /admin/meeting/minutes/{bookingId}/process
     * Body: { "audioUrl": "..." }
     */
    @PostMapping("/{bookingId}/process")
    public Result<MeetingMinutesAi> process(@PathVariable Long bookingId,
                                             @RequestBody ProcessRequest request) {
        return Result.ok(meetingMinutesAiService.requestProcess(bookingId, request.getAudioUrl()));
    }

    /**
     * 查询纪要状态 + 内容
     * GET /admin/meeting/minutes/{bookingId}
     */
    @GetMapping("/{bookingId}")
    public Result<MeetingMinutesAi> get(@PathVariable Long bookingId) {
        return Result.ok(meetingMinutesAiService.getByBookingId(bookingId));
    }

    /**
     * 查询行动项列表
     * GET /admin/meeting/minutes/{bookingId}/action-items
     */
    @GetMapping("/{bookingId}/action-items")
    public Result<List<MeetingActionItem>> listActionItems(@PathVariable Long bookingId) {
        return Result.ok(meetingMinutesAiService.listActionItems(bookingId));
    }

    /**
     * 将行动项标记为完成
     * PUT /admin/meeting/minutes/action-items/{id}/done
     */
    @PutMapping("/action-items/{id}/done")
    public Result<Void> completeActionItem(@PathVariable Long id) {
        meetingMinutesAiService.completeActionItem(id);
        return Result.ok();
    }

    @Data
    static class ProcessRequest {
        private String audioUrl;
    }
}
