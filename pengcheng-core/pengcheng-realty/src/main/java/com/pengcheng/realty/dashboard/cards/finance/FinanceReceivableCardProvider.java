package com.pengcheng.realty.dashboard.cards.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.receivable.entity.ReceivablePlan;
import com.pengcheng.realty.receivable.entity.ReceivableRecord;
import com.pengcheng.realty.receivable.mapper.ReceivablePlanMapper;
import com.pengcheng.realty.receivable.mapper.ReceivableRecordMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本月应收 vs 已收对比卡片（柱状图）
 */
@Component
@RequiredArgsConstructor
public class FinanceReceivableCardProvider implements DashboardCardProvider {

    private final ReceivablePlanMapper receivablePlanMapper;
    private final ReceivableRecordMapper receivableRecordMapper;

    @Override
    public String code() {
        return "finance.receivable";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "本月应收 vs 已收"; }
            public String category()       { return "finance"; }
            public Set<String> applicableRoles() { return Set.of("finance", "admin"); }
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 3; }
            public String suggestedChart() { return "bar"; }
            public String description()    { return "本月应收金额与实际已收金额对比"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        YearMonth ym = YearMonth.now();
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // 本月应付计划合计（dueDate 在本月内）
        List<ReceivablePlan> plans = receivablePlanMapper.selectList(new LambdaQueryWrapper<ReceivablePlan>()
                .ge(ReceivablePlan::getDueDate, monthStart)
                .le(ReceivablePlan::getDueDate, monthEnd));
        BigDecimal dueTotal = plans.stream()
                .map(ReceivablePlan::getDueAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 本月实收合计（paidDate 在本月内）
        List<ReceivableRecord> records = receivableRecordMapper.selectList(new LambdaQueryWrapper<ReceivableRecord>()
                .ge(ReceivableRecord::getPaidDate, monthStart)
                .le(ReceivableRecord::getPaidDate, monthEnd));
        BigDecimal paidTotal = records.stream()
                .map(ReceivableRecord::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "xAxis", List.of("本月"),
                "series", List.of(
                        Map.of("name", "应收", "data", List.of(dueTotal)),
                        Map.of("name", "已收", "data", List.of(paidTotal))
                )
        );
    }
}
