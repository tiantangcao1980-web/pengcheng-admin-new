package com.pengcheng.system.meeting.minutes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.asr.AsrRequest;
import com.pengcheng.ai.asr.AsrResponse;
import com.pengcheng.ai.asr.AsrService;
import com.pengcheng.ai.service.AiChatService;
import com.pengcheng.system.meeting.minutes.entity.MeetingActionItem;
import com.pengcheng.system.meeting.minutes.entity.MeetingMinutesAi;
import com.pengcheng.system.meeting.minutes.mapper.MeetingActionItemMapper;
import com.pengcheng.system.meeting.minutes.mapper.MeetingMinutesAiMapper;
import com.pengcheng.system.meeting.minutes.service.MeetingMinutesAiService;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.service.TodoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * AI 纪要服务实现（Phase 4 J5）
 *
 * <p>异步状态机：
 * <pre>
 * requestProcess() → 置 PENDING → 异步触发 processAsync()
 *   processAsync():
 *     1. TRANSCRIBING: 调 AsrService.transcribe → 写 transcript
 *     2. SUMMARIZING: 调 AiChatService.chat → 解析 JSON {summary, actionItems}
 *     3. 持久化摘要 + 行动项 → 对每个行动项调 TodoService.create → 回填 todo_id
 *     4. status = READY
 *   任何步骤异常 → status = FAILED + error_msg（不影响其他纪要）
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingMinutesAiServiceImpl implements MeetingMinutesAiService {

    private static final String PENDING = "PENDING";
    private static final String TRANSCRIBING = "TRANSCRIBING";
    private static final String SUMMARIZING = "SUMMARIZING";
    private static final String READY = "READY";
    private static final String FAILED = "FAILED";

    private static final String SUMMARY_PROMPT_TEMPLATE =
            "请以 JSON 格式总结以下会议记录，并提取行动项。\n" +
            "输出格式严格如下（不要输出任何其他内容）：\n" +
            "{\"summary\": \"...\", \"actionItems\": [{\"content\": \"...\", \"owner\": \"...\", \"dueDate\": \"YYYY-MM-DD\"}]}\n" +
            "若无行动项则 actionItems 为空数组。\n会议记录：\n";

    private final MeetingMinutesAiMapper minutesAiMapper;
    private final MeetingActionItemMapper actionItemMapper;
    private final AsrService asrService;
    private final AiChatService aiChatService;
    private final TodoService todoService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MeetingMinutesAi requestProcess(Long bookingId, String audioUrl) {
        // 幂等：已存在则重置
        MeetingMinutesAi existing = getByBookingId(bookingId);
        if (existing != null) {
            existing.setAudioUrl(audioUrl);
            existing.setStatus(PENDING);
            existing.setTranscript(null);
            existing.setSummary(null);
            existing.setErrorMsg(null);
            minutesAiMapper.updateById(existing);
            processAsync(existing.getId());
            return existing;
        }
        MeetingMinutesAi minutes = new MeetingMinutesAi();
        minutes.setBookingId(bookingId);
        minutes.setAudioUrl(audioUrl);
        minutes.setStatus(PENDING);
        minutesAiMapper.insert(minutes);
        processAsync(minutes.getId());
        return minutes;
    }

    @Override
    @Async
    public void processAsync(Long minutesId) {
        MeetingMinutesAi minutes = minutesAiMapper.selectById(minutesId);
        if (minutes == null) {
            log.warn("[AI纪要] minutesId={} 不存在，跳过", minutesId);
            return;
        }
        try {
            // Step 1: ASR 转写
            minutes.setStatus(TRANSCRIBING);
            minutesAiMapper.updateById(minutes);

            AsrRequest asrRequest = new AsrRequest();
            asrRequest.setAudioUrl(minutes.getAudioUrl());
            asrRequest.setLanguage("zh");
            AsrResponse asrResponse = asrService.transcribe(asrRequest);
            String transcript = asrResponse.getTranscript();
            minutes.setTranscript(transcript);

            // Step 2: LLM 摘要 + 行动项提取
            minutes.setStatus(SUMMARIZING);
            minutesAiMapper.updateById(minutes);

            String prompt = SUMMARY_PROMPT_TEMPLATE + transcript;
            AiChatService.ChatResult chatResult = aiChatService.chat(prompt);
            SummaryResult summaryResult = parseSummaryResult(chatResult.content());

            // Step 3: 持久化摘要
            minutes.setSummary(summaryResult.getSummary());
            minutes.setStatus(READY);
            minutesAiMapper.updateById(minutes);

            // Step 4: 创建行动项 + 关联 Todo
            if (summaryResult.getActionItems() != null) {
                for (SummaryResult.ActionItemDTO dto : summaryResult.getActionItems()) {
                    createActionItem(minutes, dto);
                }
            }
            log.info("[AI纪要] bookingId={} 处理完成，提取行动项 {} 条",
                    minutes.getBookingId(),
                    summaryResult.getActionItems() == null ? 0 : summaryResult.getActionItems().size());

        } catch (Exception e) {
            log.error("[AI纪要] bookingId={} 处理失败: {}", minutes.getBookingId(), e.getMessage(), e);
            minutes.setStatus(FAILED);
            minutes.setErrorMsg(truncate(e.getMessage(), 512));
            minutesAiMapper.updateById(minutes);
        }
    }

    @Override
    public MeetingMinutesAi getByBookingId(Long bookingId) {
        return minutesAiMapper.selectOne(
                new LambdaQueryWrapper<MeetingMinutesAi>()
                        .eq(MeetingMinutesAi::getBookingId, bookingId)
        );
    }

    @Override
    public List<MeetingActionItem> listActionItems(Long bookingId) {
        return actionItemMapper.selectList(
                new LambdaQueryWrapper<MeetingActionItem>()
                        .eq(MeetingActionItem::getBookingId, bookingId)
                        .orderByAsc(MeetingActionItem::getCreateTime)
        );
    }

    @Override
    @Transactional
    public void completeActionItem(Long actionItemId) {
        MeetingActionItem item = actionItemMapper.selectById(actionItemId);
        if (item == null) {
            return;
        }
        item.setStatus(1);
        actionItemMapper.updateById(item);
        // 同步更新关联 Todo（如已创建）
        if (item.getTodoId() != null && item.getOwnerId() != null) {
            todoService.completeTodo(item.getTodoId(), item.getOwnerId());
        }
    }

    // ---- 私有方法 ----

    /**
     * 为每个 actionItem 创建 meeting_action_item 记录并关联 sys_todo
     */
    private void createActionItem(MeetingMinutesAi minutes, SummaryResult.ActionItemDTO dto) {
        try {
            MeetingActionItem item = new MeetingActionItem();
            item.setBookingId(minutes.getBookingId());
            item.setMinutesId(minutes.getId());
            item.setContent(dto.getContent());
            item.setStatus(0);
            if (dto.getDueDate() != null && !dto.getDueDate().isBlank()) {
                try {
                    item.setDueDate(java.time.LocalDate.parse(dto.getDueDate()));
                } catch (Exception ex) {
                    log.debug("[AI纪要] dueDate 解析失败，忽略: {}", dto.getDueDate());
                }
            }
            actionItemMapper.insert(item);

            // 创建 sys_todo 并回填 todo_id
            Todo todo = new Todo();
            todo.setTitle(dto.getContent());
            todo.setDescription("来自会议 AI 纪要（预订 id=" + minutes.getBookingId() + "）");
            todo.setSourceType("meeting_action");
            todo.setSourceId(minutes.getBookingId());
            todo.setStatus(0);
            todo.setPriority(0);
            if (item.getDueDate() != null) {
                todo.setDueDate(item.getDueDate().atStartOfDay());
            }
            Todo saved = todoService.createTodo(todo);

            item.setTodoId(saved.getId());
            actionItemMapper.updateById(item);
        } catch (Exception e) {
            log.warn("[AI纪要] 行动项创建失败，跳过: content={}, err={}", dto.getContent(), e.getMessage());
        }
    }

    /**
     * 解析 LLM 返回的 JSON；解析失败则返回空结果（不抛出，由外层捕获整体 FAILED）
     */
    private SummaryResult parseSummaryResult(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM 返回内容为空");
        }
        // 提取第一个 JSON 块（LLM 可能在 JSON 前后附带说明）
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalStateException("LLM 返回内容中未找到有效 JSON: " + truncate(content, 200));
        }
        String json = content.substring(start, end + 1);
        try {
            return objectMapper.readValue(json, SummaryResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("LLM JSON 解析失败: " + e.getMessage() + ", json=" + truncate(json, 200));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ---- 内部 DTO ----

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SummaryResult {
        private String summary;
        private List<ActionItemDTO> actionItems = Collections.emptyList();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class ActionItemDTO {
            private String content;
            private String owner;
            private String dueDate;
        }
    }
}
