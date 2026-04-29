package com.pengcheng.realty.dashboard.cards.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import com.pengcheng.system.visit.entity.SalesVisit;
import com.pengcheng.system.visit.mapper.SalesVisitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本周拜访热力图：7 天 × 24h 热力数据
 */
@Component
@RequiredArgsConstructor
public class SalesActivityHeatmapCardProvider implements DashboardCardProvider {

    private final SalesVisitMapper salesVisitMapper;

    @Override
    public String code() {
        return "sales.activity";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "拜访活动热力图"; }
            public String category()       { return "sales"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 4; }
            public String suggestedChart() { return "heatmap"; }
            public String description()    { return "本周每天各小时段拜访活动热力分布"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 本周周一到周日
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDateTime rangeStart = weekStart.atStartOfDay();
        LocalDateTime rangeEnd = weekStart.plusDays(7).atStartOfDay();

        List<SalesVisit> visits = salesVisitMapper.selectList(new LambdaQueryWrapper<SalesVisit>()
                .ge(SalesVisit::getVisitTime, rangeStart)
                .lt(SalesVisit::getVisitTime, rangeEnd)
                .eq(SalesVisit::getDeleted, 0));

        // heatmap 数据格式: [[dayOfWeek(0=Mon), hour, count], ...]
        int[][] grid = new int[7][24];
        for (SalesVisit v : visits) {
            if (v.getVisitTime() == null) continue;
            int day = v.getVisitTime().getDayOfWeek().getValue() - 1; // 0=Mon
            int hour = v.getVisitTime().getHour();
            grid[day][hour]++;
        }

        List<int[]> data = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 24; h++) {
                if (grid[d][h] > 0) {
                    data.add(new int[]{d, h, grid[d][h]});
                }
            }
        }

        return Map.of(
                "data", data,
                "xAxis", List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                "yAxis", buildHourLabels()
        );
    }

    private List<String> buildHourLabels() {
        List<String> hours = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d:00", i));
        }
        return hours;
    }
}
