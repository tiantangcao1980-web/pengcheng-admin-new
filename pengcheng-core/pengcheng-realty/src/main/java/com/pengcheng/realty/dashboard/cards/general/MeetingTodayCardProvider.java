package com.pengcheng.realty.dashboard.cards.general;

import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.mapper.CalendarEventMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 今日我的会议卡片（表格）
 */
@Component
@RequiredArgsConstructor
public class MeetingTodayCardProvider implements DashboardCardProvider {

    private final CalendarEventMapper calendarEventMapper;

    @Override
    public String code() {
        return "general.meeting.today";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "今日我的会议"; }
            public String category()       { return "general"; }
            public Set<String> applicableRoles() { return Set.of(); } // 全部角色
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 4; }
            public String suggestedChart() { return "table"; }
            public String description()    { return "今日当前用户参与的所有会议列表"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        Long userId = ctx.userId();
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        List<CalendarEvent> events = calendarEventMapper.findByUserAndRange(userId, dayStart, dayEnd);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CalendarEvent e : events) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getId());
            row.put("title", e.getTitle());
            row.put("startTime", e.getStartTime());
            row.put("endTime", e.getEndTime());
            row.put("location", e.getLocation());
            row.put("status", e.getStatus());
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", List.of("主题", "开始时间", "结束时间", "地点", "状态"));
        result.put("rows", rows);
        result.put("total", rows.size());
        return result;
    }
}
