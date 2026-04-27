package com.pengcheng.system.dashboard.cards.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.receivable.entity.ReceivablePlan;
import com.pengcheng.realty.receivable.mapper.ReceivablePlanMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 逾期回款笔数卡片
 * <p>status=4（逾期）的回款计划数量
 */
@Component
@RequiredArgsConstructor
public class FinanceOverdueCardProvider implements DashboardCardProvider {

    private final ReceivablePlanMapper receivablePlanMapper;

    @Override
    public String code() {
        return "finance.overdue";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "逾期回款笔数"; }
            public String category()       { return "finance"; }
            public Set<String> applicableRoles() { return Set.of("finance", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "当前标记为逾期状态的回款计划笔数"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        long overdueCount = receivablePlanMapper.selectCount(new LambdaQueryWrapper<ReceivablePlan>()
                .eq(ReceivablePlan::getStatus, ReceivablePlan.STATUS_OVERDUE));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", overdueCount);
        result.put("unit", "笔");
        return result;
    }
}
