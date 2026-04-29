package com.pengcheng.system.meeting.minutes.service;

import com.pengcheng.system.meeting.minutes.entity.MeetingActionItem;
import com.pengcheng.system.meeting.minutes.entity.MeetingMinutesAi;

import java.util.List;

/**
 * AI 纪要服务接口（Phase 4 J5）
 *
 * <p>异步状态机：PENDING → TRANSCRIBING → SUMMARIZING → READY
 *                                         ↘ FAILED（任何步骤失败）
 */
public interface MeetingMinutesAiService {

    /**
     * 触发 AI 纪要处理（幂等：若已存在则重置为 PENDING 重新处理）
     *
     * @param bookingId 预订 id
     * @param audioUrl  录音 OSS 路径
     * @return 创建/重置后的纪要记录
     */
    MeetingMinutesAi requestProcess(Long bookingId, String audioUrl);

    /**
     * 异步执行：ASR 转写 → LLM 摘要 → 提取行动项 → 创建 Todo
     * 由 requestProcess 内部通过 @Async 调用，外部禁止直接调用。
     */
    void processAsync(Long minutesId);

    /**
     * 查询纪要详情（含状态）
     */
    MeetingMinutesAi getByBookingId(Long bookingId);

    /**
     * 查询行动项列表
     */
    List<MeetingActionItem> listActionItems(Long bookingId);

    /**
     * 将行动项标记为完成
     */
    void completeActionItem(Long actionItemId);
}
