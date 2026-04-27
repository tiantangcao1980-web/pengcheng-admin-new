package com.pengcheng.system.dashboard.cards.general;

import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import com.pengcheng.system.todo.mapper.TodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 今日/本周待办概览卡片
 */
@Component
@RequiredArgsConstructor
public class TodoOverviewCardProvider implements DashboardCardProvider {

    private final TodoMapper todoMapper;

    @Override
    public String code() {
        return "general.todo";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "我的待办"; }
            public String category()       { return "general"; }
            public Set<String> applicableRoles() { return Set.of(); } // 全部角色可见
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "当前用户待处理（待办+进行中）事项总数"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        Long userId = ctx.userId();
        int pending = todoMapper.countPending(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", pending);
        result.put("unit", "件");
        return result;
    }
}
