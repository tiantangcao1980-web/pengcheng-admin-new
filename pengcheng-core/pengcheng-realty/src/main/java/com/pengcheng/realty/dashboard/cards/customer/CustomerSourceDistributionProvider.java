package com.pengcheng.realty.dashboard.cards.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客户来源分布卡片（饼图）
 * <p>以 allianceId 是否为空区分渠道来源：
 * <ul>
 *   <li>allianceId IS NOT NULL → 渠道带客</li>
 *   <li>allianceId IS NULL → 自然到访</li>
 * </ul>
 * TODO: Phase 5 客户来源字段(source)上线后替换为按 source 字段分组统计
 */
@Component
@RequiredArgsConstructor
public class CustomerSourceDistributionProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public String code() {
        return "customer.source";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "客户来源分布"; }
            public String category()       { return "customer"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 3; }
            public String suggestedChart() { return "pie"; }
            public String description()    { return "按来源渠道展示客户分布（渠道带客 vs 自然到访）"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        long channelCount = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .isNotNull(Customer::getAllianceId));
        long naturalCount = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .isNull(Customer::getAllianceId));

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(pieOf("渠道带客", channelCount));
        series.add(pieOf("自然到访", naturalCount));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("series", series);
        result.put("isMock", false);
        return result;
    }

    private Map<String, Object> pieOf(String name, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
