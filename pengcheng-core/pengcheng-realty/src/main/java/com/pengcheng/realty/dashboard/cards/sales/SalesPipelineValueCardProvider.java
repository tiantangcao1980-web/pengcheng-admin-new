package com.pengcheng.realty.dashboard.cards.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 在谈商机金额合计卡片
 * <p>TODO: Phase 5 商机表(opportunity)上线后，替换为 OpportunityMapper.sumInProgressAmount()；
 * 当前使用 Customer 的 dealProbability 字段近似：在谈客户数（status=2）× 平均成交金额估算值。
 */
@Component
@RequiredArgsConstructor
public class SalesPipelineValueCardProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public String code() {
        return "sales.pipeline.value";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "在谈商机金额"; }
            public String category()       { return "sales"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "当前在谈客户合计潜在成交金额（单位：元）"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 在谈：status=2（已到访，尚未成交）
        long inProgressCount = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, 2));

        // TODO: Phase 5 商机表上线后替换为真实金额聚合查询
        // 当前 mock：以 100 万元 × 在谈客户数 作为估算
        BigDecimal estimatedAmount = BigDecimal.valueOf(inProgressCount).multiply(BigDecimal.valueOf(1_000_000));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", estimatedAmount);
        result.put("count", inProgressCount);
        result.put("unit", "元");
        result.put("isMock", true);
        return result;
    }
}
