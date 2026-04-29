package com.pengcheng.realty.dashboard.cards.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客户健康度分布卡片：健康 / 预警 / 沉睡
 * <p>规则：
 * <ul>
 *   <li>健康：30 天内有 lastFollowTime 且未成交</li>
 *   <li>预警：30-60 天无跟进</li>
 *   <li>沉睡：60 天以上无跟进或 lastFollowTime 为空</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CustomerHealthCardProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public String code() {
        return "customer.health";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "客户健康度分布"; }
            public String category()       { return "customer"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 3; }
            public String suggestedChart() { return "pie"; }
            public String description()    { return "按最近跟进时间分类客户健康/预警/沉睡状态分布"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime day30Ago = now.minusDays(30);
        LocalDateTime day60Ago = now.minusDays(60);

        // 健康：30天内有跟进，未成交
        long healthy = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ne(Customer::getStatus, 3)
                .ge(Customer::getLastFollowTime, day30Ago));
        // 预警：30-60天无跟进
        long warning = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ne(Customer::getStatus, 3)
                .ge(Customer::getLastFollowTime, day60Ago)
                .lt(Customer::getLastFollowTime, day30Ago));
        // 沉睡：60天以上 或 从未跟进
        long sleeping = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ne(Customer::getStatus, 3)
                .and(w -> w.lt(Customer::getLastFollowTime, day60Ago).or().isNull(Customer::getLastFollowTime)));

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(pieOf("健康", healthy));
        series.add(pieOf("预警", warning));
        series.add(pieOf("沉睡", sleeping));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("series", series);
        return result;
    }

    private Map<String, Object> pieOf(String name, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
