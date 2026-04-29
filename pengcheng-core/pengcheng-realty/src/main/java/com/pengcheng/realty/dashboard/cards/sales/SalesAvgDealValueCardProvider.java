package com.pengcheng.realty.dashboard.cards.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本月平均成交金额卡片
 */
@Component
@RequiredArgsConstructor
public class SalesAvgDealValueCardProvider implements DashboardCardProvider {

    private final CustomerDealMapper customerDealMapper;

    @Override
    public String code() {
        return "sales.avg-deal";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "本月平均成交金额"; }
            public String category()       { return "sales"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "本月所有成交记录的平均成交金额（单位：元）"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        List<CustomerDeal> deals = customerDealMapper.selectList(new LambdaQueryWrapper<CustomerDeal>()
                .ge(CustomerDeal::getDealTime, monthStart)
                .le(CustomerDeal::getDealTime, monthEnd)
                .isNotNull(CustomerDeal::getDealAmount));

        BigDecimal avg = BigDecimal.ZERO;
        if (!deals.isEmpty()) {
            BigDecimal total = deals.stream()
                    .map(CustomerDeal::getDealAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avg = total.divide(BigDecimal.valueOf(deals.size()), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", avg);
        result.put("unit", "元");
        result.put("dealCount", deals.size());
        return result;
    }
}
