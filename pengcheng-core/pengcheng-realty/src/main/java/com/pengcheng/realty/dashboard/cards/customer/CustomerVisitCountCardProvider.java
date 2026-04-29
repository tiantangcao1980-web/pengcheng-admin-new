package com.pengcheng.realty.dashboard.cards.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本周到访次数趋势（折线图）
 */
@Component
@RequiredArgsConstructor
public class CustomerVisitCountCardProvider implements DashboardCardProvider {

    private final CustomerVisitMapper customerVisitMapper;

    @Override
    public String code() {
        return "customer.visit.count";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "本周到访次数趋势"; }
            public String category()       { return "customer"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 3; }
            public String suggestedChart() { return "line"; }
            public String description()    { return "本周每日客户到访次数折线趋势"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        List<String> xAxis = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            long count = customerVisitMapper.selectCount(new LambdaQueryWrapper<CustomerVisit>()
                    .ge(CustomerVisit::getActualVisitTime, dayStart)
                    .lt(CustomerVisit::getActualVisitTime, dayEnd));

            xAxis.add(day.toString().substring(5)); // MM-dd
            values.add(count);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("xAxis", xAxis);
        result.put("series", List.of(Map.of("name", "到访次数", "data", values)));
        return result;
    }
}
