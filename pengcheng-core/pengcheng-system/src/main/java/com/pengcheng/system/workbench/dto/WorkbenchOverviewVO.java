package com.pengcheng.system.workbench.dto;

import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.project.entity.PmTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 个人工作台聚合视图
 *
 * 一屏整合：日历 + 待办 + 项目任务 + 关键计数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchOverviewVO {

    private LocalDate date;

    /** 今日日历事件 */
    private List<CalendarEvent> todayEvents;

    /** 我的待办（未完成） */
    private List<Todo> openTodos;

    /** 我负责的项目任务（未完成） */
    private List<PmTask> myProjectTasks;

    /** 关键计数 */
    private Counts counts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counts {
        private int todayEventCount;
        private int openTodoCount;
        private int myProjectTaskCount;
    }
}
