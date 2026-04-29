package com.pengcheng.realty.dashboard.cards.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
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
 * 近 30 天新增客户趋势（折线图）
 */
@Component
@RequiredArgsConstructor
public class NewCustomerTrendCardProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public String code() {
        return "customer.new.trend";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "新增客户趋势"; }
            public String category()       { return "customer"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 3; }
            public String suggestedChart() { return "line"; }
            public String description()    { return "近 30 天每日新增客户数量趋势"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        LocalDate today = LocalDate.now();
        List<String> xAxis = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            long count = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                    .ge(Customer::getCreateTime, dayStart)
                    .lt(Customer::getCreateTime, dayEnd));

            xAxis.add(day.toString().substring(5)); // MM-dd
            values.add(count);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("xAxis", xAxis);
        result.put("series", List.of(Map.of("name", "新增客户", "data", values)));
        return result;
    }
}
