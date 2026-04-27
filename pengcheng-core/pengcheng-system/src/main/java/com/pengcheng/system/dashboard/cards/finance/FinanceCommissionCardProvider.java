package com.pengcheng.system.dashboard.cards.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本月待结佣金卡片
 * <p>auditStatus=2（审核通过）且 payableAmount 未清零的佣金之和。
 */
@Component
@RequiredArgsConstructor
public class FinanceCommissionCardProvider implements DashboardCardProvider {

    private final CommissionMapper commissionMapper;

    @Override
    public String code() {
        return "finance.commission";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "本月待结佣金"; }
            public String category()       { return "finance"; }
            public Set<String> applicableRoles() { return Set.of("finance", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "本月审核通过待结算的应结佣金总额"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        // auditStatus=2 审核通过，待结佣
        List<Commission> commissions = commissionMapper.selectList(new LambdaQueryWrapper<Commission>()
                .eq(Commission::getAuditStatus, 2)
                .ge(Commission::getCreateTime, monthStart)
                .le(Commission::getCreateTime, monthEnd)
                .isNotNull(Commission::getPayableAmount));

        BigDecimal total = commissions.stream()
                .map(Commission::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", total);
        result.put("unit", "元");
        result.put("count", commissions.size());
        return result;
    }
}
