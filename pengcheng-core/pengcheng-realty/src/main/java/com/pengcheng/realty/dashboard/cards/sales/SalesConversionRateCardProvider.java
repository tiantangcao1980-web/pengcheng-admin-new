package com.pengcheng.realty.dashboard.cards.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 本月转化率卡片：成交数 / 线索数（本月新增）
 */
@Component
@RequiredArgsConstructor
public class SalesConversionRateCardProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public String code() {
        return "sales.conversion";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "本月转化率"; }
            public String category()       { return "sales"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "本月新增线索中最终成交的转化比率（%）"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        long totalLeads = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ge(Customer::getCreateTime, monthStart)
                .le(Customer::getCreateTime, monthEnd));

        long dealCount = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, 3)
                .ge(Customer::getCreateTime, monthStart)
                .le(Customer::getCreateTime, monthEnd));

        double rate = totalLeads == 0 ? 0.0 : Math.round(dealCount * 10000.0 / totalLeads) / 100.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", rate);
        result.put("unit", "%");
        result.put("dealCount", dealCount);
        result.put("leadCount", totalLeads);
        return result;
    }
}
