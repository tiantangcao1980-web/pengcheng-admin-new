package com.pengcheng.realty.dashboard.cards.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerPoolEventLog;
import com.pengcheng.realty.customer.mapper.CustomerPoolEventLogMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 公海池容量 + 30 天内回收卡片
 * <p>poolType=1 为公海客户；event_type=recycle 为回收至公海事件
 */
@Component
@RequiredArgsConstructor
public class CustomerPoolCardProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;
    private final CustomerPoolEventLogMapper poolEventLogMapper;

    @Override
    public String code() {
        return "customer.pool";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "公海池概况"; }
            public String category()       { return "customer"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "公海池当前客户容量及近 30 天回收至公海客户数"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 公海池当前容量
        long poolSize = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPoolType, 1));

        // 近30天回收至公海的客户数
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long recycled30d = poolEventLogMapper.selectCount(new LambdaQueryWrapper<CustomerPoolEventLog>()
                .eq(CustomerPoolEventLog::getEventType, CustomerPoolEventLog.EVENT_TYPE_RECYCLE)
                .ge(CustomerPoolEventLog::getEventTime, since));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("poolSize", poolSize);
        result.put("recycled30d", recycled30d);
        return result;
    }
}
