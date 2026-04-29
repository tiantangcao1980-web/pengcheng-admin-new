package com.pengcheng.system.smarttable.automation.action;

import com.pengcheng.system.smarttable.automation.AutomationEvent;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 动作：创建待办（CREATE_TODO）
 *
 * <p>params 字段说明：
 * <pre>
 * {
 *   "title":      "待办标题，支持 {{fieldKey}} 占位符",
 *   "assigneeId": 123,    // 指派用户 ID（可选）
 *   "priority":   1       // 0=普通,1=重要,2=紧急（可选）
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTodoAction implements AutomationAction {

    private final TodoService todoService;

    @Override
    public String type() {
        return "CREATE_TODO";
    }

    @Override
    public void execute(Map<String, Object> params, AutomationEvent event) {
        String title = renderTemplate((String) params.getOrDefault("title", "自动化待办"), event);
        Long assigneeId = toLong(params.get("assigneeId"));
        Integer priority = toInt(params.get("priority"), 0);

        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setSourceType("automation");
        todo.setSourceId(event.getTableId());
        todo.setPriority(priority);
        todo.setStatus(0);
        if (assigneeId != null) {
            todo.setUserId(assigneeId);
            todo.setAssigneeId(assigneeId);
        }

        todoService.createTodo(todo);
        log.info("[Automation] CREATE_TODO 成功: tableId={}, recordId={}, title={}",
                event.getTableId(), event.getRecordId(), title);
    }

    /** 将 {{fieldKey}} 替换为 newRow 中对应值 */
    private String renderTemplate(String template, AutomationEvent event) {
        if (template == null || event.getNewRow() == null) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : event.getNewRow().entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return result;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return defaultVal; }
    }
}
