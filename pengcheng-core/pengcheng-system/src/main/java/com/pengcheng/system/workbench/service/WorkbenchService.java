package com.pengcheng.system.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.service.CalendarService;
import com.pengcheng.system.project.entity.PmTask;
import com.pengcheng.system.project.mapper.PmTaskMapper;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.service.TodoService;
import com.pengcheng.system.workbench.dto.WorkbenchOverviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 个人工作台聚合服务
 *
 * 一次查询返回 calendar + todo + project task 三类数据，前端一屏展示。
 * 任意子查询失败安全降级为空集合，不阻塞整体加载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbenchService {

    private final CalendarService calendarService;
    private final TodoService todoService;
    private final PmTaskMapper pmTaskMapper;

    /**
     * 工作台聚合视图
     *
     * @param userId 用户ID
     * @param date 视图日期（默认今天）
     */
    public WorkbenchOverviewVO getOverview(Long userId, LocalDate date) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        LocalDate target = date == null ? LocalDate.now() : date;

        List<CalendarEvent> events = safeFetch(
                () -> calendarService.getEvents(userId, target, target),
                "calendar");

        // status=0 表示未完成（约定）
        List<Todo> todos = safeFetch(
                () -> todoService.getUserTodos(userId, 0),
                "todo");

        List<PmTask> projectTasks = safeFetch(
                () -> pmTaskMapper.selectList(new LambdaQueryWrapper<PmTask>()
                        .eq(PmTask::getAssigneeId, userId)
                        .ne(PmTask::getStatus, "DONE")
                        .orderByDesc(PmTask::getPriority)
                        .orderByAsc(PmTask::getDueDate)),
                "pmTask");

        return WorkbenchOverviewVO.builder()
                .date(target)
                .todayEvents(events)
                .openTodos(todos)
                .myProjectTasks(projectTasks)
                .counts(WorkbenchOverviewVO.Counts.builder()
                        .todayEventCount(events.size())
                        .openTodoCount(todos.size())
                        .myProjectTaskCount(projectTasks.size())
                        .build())
                .build();
    }

    private <T> List<T> safeFetch(java.util.function.Supplier<List<T>> supplier, String name) {
        try {
            List<T> result = supplier.get();
            return result == null ? Collections.emptyList() : result;
        } catch (Exception e) {
            log.warn("[Workbench] 加载 {} 失败，降级为空: {}", name, e.getMessage());
            return Collections.emptyList();
        }
    }
}
