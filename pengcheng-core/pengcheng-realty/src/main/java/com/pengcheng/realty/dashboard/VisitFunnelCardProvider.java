package com.pengcheng.realty.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客户到访漏斗卡片：首访 → 二次访 → 认购 → 签约，4 阶段客户数
 *
 * <p>数据逻辑：
 * <ul>
 *   <li>首访：时间窗内 visitCount >= 1 的客户（customer.visit_count 字段）</li>
 *   <li>二访：时间窗内 visitCount >= 2 的客户</li>
 *   <li>认购：customer_deal.subscribe_type 不为 null 的成交记录对应客户</li>
 *   <li>签约：customer_deal.sign_status = 1（已签约）的成交记录对应客户</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class VisitFunnelCardProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;
    private final CustomerDealMapper customerDealMapper;

    @Override
    public String code() {
        return "realty.visit-funnel";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()                  { return "客户到访漏斗"; }
            public String category()              { return "realty"; }
            public Set<String> applicableRoles()  { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()              { return 6; }
            public int defaultRows()              { return 4; }
            public String suggestedChart()        { return "funnel"; }
            public String description()           { return "首访→二访→认购→签约四阶段客户转化漏斗"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 首访：visitCount >= 1，且 visitTime 在时间窗内
        long firstVisit = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ge(Customer::getVisitCount, 1)
                .ge(ctx.windowStart() != null, Customer::getVisitTime, ctx.windowStart())
                .le(ctx.windowEnd() != null, Customer::getVisitTime, ctx.windowEnd()));

        // 二访：visitCount >= 2，时间窗同上
        long secondVisit = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ge(Customer::getVisitCount, 2)
                .ge(ctx.windowStart() != null, Customer::getVisitTime, ctx.windowStart())
                .le(ctx.windowEnd() != null, Customer::getVisitTime, ctx.windowEnd()));

        // 认购：customer_deal.subscribe_type NOT NULL
        // SQL 等价：SELECT COUNT(DISTINCT customer_id) FROM customer_deal WHERE subscribe_type IS NOT NULL AND create_time BETWEEN ...
        List<CustomerDeal> subscribeDeals = customerDealMapper.selectList(new LambdaQueryWrapper<CustomerDeal>()
                .isNotNull(CustomerDeal::getSubscribeType)
                .ge(ctx.windowStart() != null, CustomerDeal::getDealTime, ctx.windowStart())
                .le(ctx.windowEnd() != null, CustomerDeal::getDealTime, ctx.windowEnd()));
        long subscribed = subscribeDeals.stream().map(CustomerDeal::getCustomerId).distinct().count();

        // 签约：customer_deal.sign_status = 1（已签约）
        List<CustomerDeal> signedDeals = customerDealMapper.selectList(new LambdaQueryWrapper<CustomerDeal>()
                .eq(CustomerDeal::getSignStatus, 1)
                .ge(ctx.windowStart() != null, CustomerDeal::getDealTime, ctx.windowStart())
                .le(ctx.windowEnd() != null, CustomerDeal::getDealTime, ctx.windowEnd()));
        long signed = signedDeals.stream().map(CustomerDeal::getCustomerId).distinct().count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stages", new Object[]{
                stageOf("首访", firstVisit),
                stageOf("二访", secondVisit),
                stageOf("认购", subscribed),
                stageOf("签约", signed)
        });
        return result;
    }

    private Map<String, Object> stageOf(String name, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
