package com.pengcheng.realty.dashboard.cards.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 销售漏斗卡片：线索→意向→报名→成交→回款 5 阶段计数
 * <p>使用 customer.status 字段：1-已报备(线索) 2-已到访(意向/报名) 3-已成交
 * payment_status: 0-未回款 1-部分 2-全部
 */
@Component
@RequiredArgsConstructor
public class SalesFunnelCardProvider implements DashboardCardProvider {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public String code() {
        return "sales.funnel";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "销售漏斗"; }
            public String category()       { return "sales"; }
            public Set<String> applicableRoles() { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 4; }
            public String suggestedChart() { return "funnel"; }
            public String description()    { return "展示线索→意向→报名→成交→回款五阶段客户转化数量"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 线索（已报备）
        long lead = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ge(Customer::getStatus, 1));
        // 意向（已到访及以上）
        long intent = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ge(Customer::getStatus, 2));
        // 报名：这里用 status>=2（到访视为报名阶段）同 intent 近似，后续可细化
        long signup = intent;
        // 成交
        long deal = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, 3));
        // 回款：已成交且 dealProbability >= 1（全款到账近似），或由 CustomerDeal 判断
        // 此处仅用 deal 数作兜底
        long payment = deal;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stages", new Object[]{
                stageOf("线索", lead),
                stageOf("意向", intent),
                stageOf("报名", signup),
                stageOf("成交", deal),
                stageOf("回款", payment)
        });
        return result;
    }

    private Map<String, Object> stageOf(String name, long count) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", count);
        return m;
    }
}
