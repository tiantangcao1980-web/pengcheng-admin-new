package com.pengcheng.system.meeting.minutes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.meeting.minutes.entity.MeetingMinutesAi;
import com.pengcheng.system.meeting.minutes.mapper.MeetingActionItemMapper;
import com.pengcheng.system.meeting.minutes.mapper.MeetingMinutesAiMapper;
import com.pengcheng.system.meeting.minutes.service.impl.MeetingMinutesAiServiceImpl;
import com.pengcheng.system.todo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MeetingMinutesAiServiceImpl 单元测试（Phase 4 J5）。
 *
 * <p>AsrService / AiChatService 在 pengcheng-ai 模块（system 不能反向依赖）。
 * Service 改用 ApplicationContext 反射软依赖。本单测仅校验：
 * <ul>
 *   <li>requestProcess 创建 MinutesAi 记录</li>
 *   <li>requestProcess 幂等：已存在 bookingId 时重置而非新建</li>
 *   <li>processAsync 软依赖降级：ai 模块不在 classpath 时不抛、status 安全推进</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingMinutesAiServiceImpl")
class MeetingMinutesAiServiceImplTest {

    @Mock
    private MeetingMinutesAiMapper minutesAiMapper;
    @Mock
    private MeetingActionItemMapper actionItemMapper;
    @Mock
    private TodoService todoService;
    @Mock
    private ApplicationContext applicationContext;

    private MeetingMinutesAiServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MeetingMinutesAiServiceImpl(
                minutesAiMapper, actionItemMapper, todoService,
                new ObjectMapper(), applicationContext);
    }

    @Test
    @DisplayName("1. requestProcess：新 booking → INSERT MinutesAi 状态 PENDING")
    void requestProcess_newBooking_inserts() {
        when(minutesAiMapper.selectOne(any())).thenReturn(null);

        MeetingMinutesAi result = service.requestProcess(100L, "https://oss/audio.mp3");

        assertThat(result.getBookingId()).isEqualTo(100L);
        assertThat(result.getAudioUrl()).isEqualTo("https://oss/audio.mp3");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(minutesAiMapper).insert(any(MeetingMinutesAi.class));
    }

    @Test
    @DisplayName("2. requestProcess：已存在 booking → 幂等重置（updateById 而非 insert）")
    void requestProcess_existingBooking_isIdempotent() {
        MeetingMinutesAi existing = new MeetingMinutesAi();
        existing.setId(1L);
        existing.setBookingId(100L);
        existing.setStatus("FAILED");
        existing.setErrorMsg("旧错误");

        when(minutesAiMapper.selectOne(any())).thenReturn(existing);

        MeetingMinutesAi result = service.requestProcess(100L, "https://oss/new-audio.mp3");

        // 重置字段
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getErrorMsg()).isNull();
        assertThat(result.getAudioUrl()).isEqualTo("https://oss/new-audio.mp3");
        verify(minutesAiMapper).updateById(any(MeetingMinutesAi.class));
        verify(minutesAiMapper, never()).insert(any(MeetingMinutesAi.class));
    }

    @Test
    @DisplayName("3. processAsync：AI 模块不在 classpath → 软依赖降级，写占位 summary，status=READY")
    void processAsync_softDependencyDegrades() {
        MeetingMinutesAi minutes = new MeetingMinutesAi();
        minutes.setId(1L);
        minutes.setBookingId(100L);
        minutes.setAudioUrl("https://oss/audio.mp3");
        minutes.setStatus("PENDING");

        when(minutesAiMapper.selectById(1L)).thenReturn(minutes);
        // ApplicationContext.getBean 抛异常模拟 ai 模块不在 classpath
        org.mockito.Mockito.lenient().when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new RuntimeException("AsrService not in classpath"));

        service.processAsync(1L);

        // 应平滑推进到 READY（降级 transcript + summary 占位）
        assertThat(minutes.getStatus()).isEqualTo("READY");
        assertThat(minutes.getTranscript()).contains("ASR 服务未启用");
        assertThat(minutes.getSummary()).contains("AI 摘要服务未启用");
    }

    @Test
    @DisplayName("4. processAsync：minutesId 不存在直接 return（log warn 不抛）")
    void processAsync_notFound_returnsSilently() {
        when(minutesAiMapper.selectById(999L)).thenReturn(null);
        service.processAsync(999L); // 不应抛异常
        verify(minutesAiMapper, never()).updateById(any());
    }
}
