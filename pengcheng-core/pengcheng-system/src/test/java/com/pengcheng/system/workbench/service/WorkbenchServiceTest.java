package com.pengcheng.system.workbench.service;

import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.service.CalendarService;
import com.pengcheng.system.project.entity.PmTask;
import com.pengcheng.system.project.mapper.PmTaskMapper;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.service.TodoService;
import com.pengcheng.system.workbench.dto.WorkbenchOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("WorkbenchService — 个人工作台聚合")
class WorkbenchServiceTest {

    private CalendarService calendarService;
    private TodoService todoService;
    private PmTaskMapper pmTaskMapper;
    private WorkbenchService service;

    @BeforeEach
    void setUp() {
        calendarService = mock(CalendarService.class);
        todoService = mock(TodoService.class);
        pmTaskMapper = mock(PmTaskMapper.class);
        service = new WorkbenchService(calendarService, todoService, pmTaskMapper);
    }

    @Test
    @DisplayName("正常聚合：3 个数据源拼装 + counts 正确")
    void overview_aggregatesThreeSources() {
        when(calendarService.getEvents(anyLong(), any(), any()))
                .thenReturn(List.of(new CalendarEvent(), new CalendarEvent()));
        when(todoService.getUserTodos(anyLong(), anyInt()))
                .thenReturn(List.of(new Todo(), new Todo(), new Todo()));
        when(pmTaskMapper.selectList(any()))
                .thenReturn(List.of(new PmTask()));

        WorkbenchOverviewVO vo = service.getOverview(1L, LocalDate.of(2026, 4, 28));

        assertThat(vo.getDate()).isEqualTo(LocalDate.of(2026, 4, 28));
        assertThat(vo.getTodayEvents()).hasSize(2);
        assertThat(vo.getOpenTodos()).hasSize(3);
        assertThat(vo.getMyProjectTasks()).hasSize(1);
        assertThat(vo.getCounts().getTodayEventCount()).isEqualTo(2);
        assertThat(vo.getCounts().getOpenTodoCount()).isEqualTo(3);
        assertThat(vo.getCounts().getMyProjectTaskCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("date 为 null 时取 LocalDate.now()")
    void overview_defaultsToToday() {
        when(calendarService.getEvents(anyLong(), any(), any())).thenReturn(List.of());
        when(todoService.getUserTodos(anyLong(), anyInt())).thenReturn(List.of());
        when(pmTaskMapper.selectList(any())).thenReturn(List.of());

        WorkbenchOverviewVO vo = service.getOverview(1L, null);
        assertThat(vo.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("userId 为 null 抛异常")
    void overview_userIdRequired() {
        assertThatThrownBy(() -> service.getOverview(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    @DisplayName("子查询失败安全降级为空集合，整体不抛")
    void overview_partialFailureGraceful() {
        when(calendarService.getEvents(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("calendar down"));
        when(todoService.getUserTodos(anyLong(), anyInt()))
                .thenReturn(List.of(new Todo()));
        when(pmTaskMapper.selectList(any()))
                .thenThrow(new RuntimeException("pm db down"));

        WorkbenchOverviewVO vo = service.getOverview(1L, LocalDate.now());

        assertThat(vo.getTodayEvents()).isEmpty();
        assertThat(vo.getOpenTodos()).hasSize(1);
        assertThat(vo.getMyProjectTasks()).isEmpty();
        assertThat(vo.getCounts().getOpenTodoCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Service 返回 null 时降级为空集合")
    void overview_nullReturnsBecomeEmpty() {
        when(calendarService.getEvents(anyLong(), any(), any())).thenReturn(null);
        when(todoService.getUserTodos(anyLong(), anyInt())).thenReturn(null);
        when(pmTaskMapper.selectList(any())).thenReturn(null);

        WorkbenchOverviewVO vo = service.getOverview(1L, LocalDate.now());

        assertThat(vo.getTodayEvents()).isEmpty();
        assertThat(vo.getOpenTodos()).isEmpty();
        assertThat(vo.getMyProjectTasks()).isEmpty();
        assertThat(vo.getCounts().getTodayEventCount()).isEqualTo(0);
    }
}
