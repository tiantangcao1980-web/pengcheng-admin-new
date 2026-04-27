package com.pengcheng.system.meeting.minutes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.asr.AsrRequest;
import com.pengcheng.ai.asr.AsrResponse;
import com.pengcheng.ai.asr.AsrService;
import com.pengcheng.ai.service.AiChatService;
import com.pengcheng.system.meeting.minutes.entity.MeetingActionItem;
import com.pengcheng.system.meeting.minutes.entity.MeetingMinutesAi;
import com.pengcheng.system.meeting.minutes.mapper.MeetingActionItemMapper;
import com.pengcheng.system.meeting.minutes.mapper.MeetingMinutesAiMapper;
import com.pengcheng.system.meeting.minutes.service.impl.MeetingMinutesAiServiceImpl;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MeetingMinutesAiServiceImpl 单元测试（Phase 4 J5）
 * 6 个用例：
 *   1. 正常异步链路（mock ASR+LLM）
 *   2. JSON 解析正常 → 写 actionItem
 *   3. LLM 返回非法 JSON → status=FAILED
 *   4. actionItem 创建 Todo 并回填 todo_id
 *   5. ASR 失败 → status=FAILED
 *   6. 已存在 booking 重新触发 → 幂等重置 PENDING
 */
@ExtendWith(MockitoExtension.class)
class MeetingMinutesAiServiceImplTest {

    @Mock
    private MeetingMinutesAiMapper minutesAiMapper;
    @Mock
    private MeetingActionItemMapper actionItemMapper;
    @Mock
    private AsrService asrService;
    @Mock
    private AiChatService aiChatService;
    @Mock
    private TodoService todoService;

    private MeetingMinutesAiServiceImpl service;

    private static final String VALID_LLM_RESPONSE =
            "{\"summary\": \"本次会议讨论了产品路线图\", \"actionItems\": [{\"content\": \"更新文档\", \"owner\": \"张三\", \"dueDate\": \"2026-05-01\"},{\"content\": \"提交代码\", \"owner\": \"\", \"dueDate\": \"\"}]}";

    @BeforeEach
    void setUp() {
        service = new MeetingMinutesAiServiceImpl(
                minutesAiMapper, actionItemMapper, asrService, aiChatService,
                todoService, new ObjectMapper()
        );
    }

    // ---- 用例 1：正常异步链路 ----
    @Test
    void processAsync_shouldTranscribeAndSummarize_whenAllSucceed() {
        MeetingMinutesAi minutes = buildMinutes(1L, 10L, "PENDING");
        when(minutesAiMapper.selectById(1L)).thenReturn(minutes);
        when(asrService.transcribe(any(AsrRequest.class)))
                .thenReturn(new AsrResponse("会议转写文本", 300L, "mock"));
        when(aiChatService.chat(anyString()))
                .thenReturn(new AiChatService.ChatResult(VALID_LLM_RESPONSE, "text"));
        when(todoService.createTodo(any())).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            t.setId(999L);
            return t;
        });

        service.processAsync(1L);

        ArgumentCaptor<MeetingMinutesAi> captor = ArgumentCaptor.forClass(MeetingMinutesAi.class);
        verify(minutesAiMapper, atLeast(2)).updateById(captor.capture());
        List<MeetingMinutesAi> updates = captor.getAllValues();
        // 最后一次 update 应为 READY
        assertEquals("READY", updates.get(updates.size() - 1).getStatus());
        assertNotNull(updates.get(updates.size() - 1).getSummary());
    }

    // ---- 用例 2：JSON 解析正常 → 写 actionItem ----
    @Test
    void processAsync_shouldPersistActionItems_whenJsonValid() {
        MeetingMinutesAi minutes = buildMinutes(2L, 20L, "PENDING");
        when(minutesAiMapper.selectById(2L)).thenReturn(minutes);
        when(asrService.transcribe(any())).thenReturn(new AsrResponse("文本", 100L, "mock"));
        when(aiChatService.chat(anyString()))
                .thenReturn(new AiChatService.ChatResult(VALID_LLM_RESPONSE, "text"));
        when(todoService.createTodo(any())).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            t.setId(1001L);
            return t;
        });

        service.processAsync(2L);

        // 2 个 actionItem 写入
        verify(actionItemMapper, times(2)).insert(any(MeetingActionItem.class));
        // 回填 todo_id → 2 次 updateById
        verify(actionItemMapper, times(2)).updateById(any(MeetingActionItem.class));
    }

    // ---- 用例 3：LLM 返回非法 JSON → status=FAILED ----
    @Test
    void processAsync_shouldSetFailed_whenLlmReturnsInvalidJson() {
        MeetingMinutesAi minutes = buildMinutes(3L, 30L, "PENDING");
        when(minutesAiMapper.selectById(3L)).thenReturn(minutes);
        when(asrService.transcribe(any())).thenReturn(new AsrResponse("文本", 100L, "mock"));
        when(aiChatService.chat(anyString()))
                .thenReturn(new AiChatService.ChatResult("这不是JSON", "text"));

        service.processAsync(3L);

        ArgumentCaptor<MeetingMinutesAi> captor = ArgumentCaptor.forClass(MeetingMinutesAi.class);
        verify(minutesAiMapper, atLeastOnce()).updateById(captor.capture());
        MeetingMinutesAi last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals("FAILED", last.getStatus());
        assertNotNull(last.getErrorMsg());
    }

    // ---- 用例 4：actionItem 创建 Todo 并回填 todo_id ----
    @Test
    void processAsync_shouldFillTodoIdAfterTodoCreation() {
        MeetingMinutesAi minutes = buildMinutes(4L, 40L, "PENDING");
        when(minutesAiMapper.selectById(4L)).thenReturn(minutes);
        when(asrService.transcribe(any())).thenReturn(new AsrResponse("文本", 100L, "mock"));
        String singleItem = "{\"summary\": \"摘要\", \"actionItems\": [{\"content\": \"安排评审会\", \"owner\": \"李四\", \"dueDate\": \"2026-05-10\"}]}";
        when(aiChatService.chat(anyString()))
                .thenReturn(new AiChatService.ChatResult(singleItem, "text"));
        when(todoService.createTodo(any())).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            t.setId(777L);
            return t;
        });

        service.processAsync(4L);

        ArgumentCaptor<MeetingActionItem> captor = ArgumentCaptor.forClass(MeetingActionItem.class);
        verify(actionItemMapper).updateById(captor.capture());
        assertEquals(777L, captor.getValue().getTodoId());
        assertEquals("安排评审会", captor.getValue().getContent());
    }

    // ---- 用例 5：ASR 失败 → status=FAILED ----
    @Test
    void processAsync_shouldSetFailed_whenAsrThrows() {
        MeetingMinutesAi minutes = buildMinutes(5L, 50L, "PENDING");
        when(minutesAiMapper.selectById(5L)).thenReturn(minutes);
        when(asrService.transcribe(any())).thenThrow(new RuntimeException("ASR 服务不可用"));

        service.processAsync(5L);

        ArgumentCaptor<MeetingMinutesAi> captor = ArgumentCaptor.forClass(MeetingMinutesAi.class);
        verify(minutesAiMapper, atLeastOnce()).updateById(captor.capture());
        MeetingMinutesAi last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals("FAILED", last.getStatus());
        assertTrue(last.getErrorMsg().contains("ASR"));
        // LLM 不应被调用
        verify(aiChatService, never()).chat(anyString());
    }

    // ---- 用例 6：已存在 booking 重新触发 → 幂等重置 PENDING ----
    @Test
    void requestProcess_shouldResetExistingRecord_whenAlreadyExists() {
        MeetingMinutesAi existing = buildMinutes(6L, 60L, "READY");
        existing.setSummary("旧摘要");
        when(minutesAiMapper.selectOne(any())).thenReturn(existing);
        // processAsync 内部需要 selectById → 返回重置后的记录
        MeetingMinutesAi resetRecord = buildMinutes(6L, 60L, "PENDING");
        when(minutesAiMapper.selectById(6L)).thenReturn(resetRecord);
        when(asrService.transcribe(any())).thenReturn(new AsrResponse("新文本", 100L, "mock"));
        when(aiChatService.chat(anyString()))
                .thenReturn(new AiChatService.ChatResult(VALID_LLM_RESPONSE, "text"));
        when(todoService.createTodo(any())).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            t.setId(888L);
            return t;
        });

        MeetingMinutesAi result = service.requestProcess(60L, "oss://new-audio.mp3");

        // 应更新（重置）而非插入
        verify(minutesAiMapper).updateById(existing);
        verify(minutesAiMapper, never()).insert(any());
        assertEquals("PENDING", existing.getStatus());
        assertNull(existing.getSummary());
    }

    // ---- 工具方法 ----
    private MeetingMinutesAi buildMinutes(Long id, Long bookingId, String status) {
        MeetingMinutesAi m = new MeetingMinutesAi();
        m.setId(id);
        m.setBookingId(bookingId);
        m.setAudioUrl("oss://audio.mp3");
        m.setStatus(status);
        return m;
    }
}
