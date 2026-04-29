package com.pengcheng.system.meeting.minutes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * AI 纪要服务实现（Phase 4 J5）。
 *
 * <p><b>跨模块解耦</b>：AsrService / AiChatService 在 pengcheng-ai 模块，
 * pengcheng-system 不能反向依赖（会与 ai → system 形成循环）。
 * 通过 ApplicationContext 反射软依赖：
 * <ul>
 *   <li>classpath 含 pengcheng-ai 时正常调用</li>
 *   <li>缺失时降级为只入库 transcript/summary 为空 + status=READY，留 TODO</li>
 * </ul>
 *
 * <p>异步状态机：PENDING → TRANSCRIBING → SUMMARIZING → READY/FAILED
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
    private final TodoService todoService;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    @Override
    @Transactional
    public MeetingMinutesAi requestProcess(Long bookingId, String audioUrl) {
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
            // Step 1: ASR 转写（软依赖）
            minutes.setStatus(TRANSCRIBING);
            minutesAiMapper.updateById(minutes);
            String transcript = invokeAsrTranscribe(minutes.getAudioUrl());
            minutes.setTranscript(transcript);

            // Step 2: LLM 摘要（软依赖）
            minutes.setStatus(SUMMARIZING);
            minutesAiMapper.updateById(minutes);
            SummaryResult summaryResult = invokeAiSummarize(transcript);

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
                        .eq(MeetingMinutesAi::getBookingId, bookingId));
    }

    @Override
    public List<MeetingActionItem> listActionItems(Long bookingId) {
        return actionItemMapper.selectList(
                new LambdaQueryWrapper<MeetingActionItem>()
                        .eq(MeetingActionItem::getBookingId, bookingId)
                        .orderByAsc(MeetingActionItem::getCreateTime));
    }

    @Override
    @Transactional
    public void completeActionItem(Long actionItemId) {
        MeetingActionItem item = actionItemMapper.selectById(actionItemId);
        if (item == null) return;
        item.setStatus(1);
        actionItemMapper.updateById(item);
        if (item.getTodoId() != null && item.getOwnerId() != null) {
            todoService.completeTodo(item.getTodoId(), item.getOwnerId());
        }
    }

    /* ===== 跨模块软依赖（反射调 pengcheng-ai） ===== */

    private String invokeAsrTranscribe(String audioUrl) {
        try {
            Class<?> reqClass = Class.forName("com.pengcheng.ai.asr.AsrRequest");
            Class<?> respClass = Class.forName("com.pengcheng.ai.asr.AsrResponse");
            Class<?> svcClass = Class.forName("com.pengcheng.ai.asr.AsrService");
            Object svc = applicationContext.getBean(svcClass);
            Object req = reqClass.getConstructor().newInstance();
            reqClass.getMethod("setAudioUrl", String.class).invoke(req, audioUrl);
            reqClass.getMethod("setLanguage", String.class).invoke(req, "zh");
            Object resp = svcClass.getMethod("transcribe", reqClass).invoke(svc, req);
            return (String) respClass.getMethod("getTranscript").invoke(resp);
        } catch (Throwable e) {
            log.warn("[AI纪要] AsrService 不可用（pengcheng-ai 未在 classpath 或未启用），降级为空 transcript: {}",
                    e.getMessage());
            return "[ASR 服务未启用，转写降级 — TODO 启用 pengcheng-ai 模块]";
        }
    }

    private SummaryResult invokeAiSummarize(String transcript) {
        try {
            Class<?> svcClass = Class.forName("com.pengcheng.ai.service.AiChatService");
            Class<?> resultClass = Class.forName("com.pengcheng.ai.service.AiChatService$ChatResult");
            Object svc = applicationContext.getBean(svcClass);
            String prompt = SUMMARY_PROMPT_TEMPLATE + transcript;
            Object chatResult = svcClass.getMethod("chat", String.class).invoke(svc, prompt);
            String content = (String) resultClass.getMethod("content").invoke(chatResult);
            return parseSummaryResult(content);
        } catch (Throwable e) {
            log.warn("[AI纪要] AiChatService 不可用，降级为占位摘要: {}", e.getMessage());
            SummaryResult fallback = new SummaryResult();
            fallback.setSummary("[AI 摘要服务未启用 — TODO 启用 pengcheng-ai 模块。原始转写见 transcript 字段。]");
            fallback.setActionItems(Collections.emptyList());
            return fallback;
        }
    }

    /* ===== 私有方法 ===== */

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

    private SummaryResult parseSummaryResult(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM 返回内容为空");
        }
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
