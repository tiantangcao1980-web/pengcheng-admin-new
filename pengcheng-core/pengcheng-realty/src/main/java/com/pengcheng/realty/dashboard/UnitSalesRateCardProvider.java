package com.pengcheng.realty.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.unit.entity.RealtyUnit;
import com.pengcheng.realty.unit.mapper.RealtyUnitMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 去化率卡片：已售（SOLD+SIGNED+SUBSCRIBED）/ 总房源数，gauge 图展示
 */
@Component
@RequiredArgsConstructor
public class UnitSalesRateCardProvider implements DashboardCardProvider {

    private final RealtyUnitMapper realtyUnitMapper;

    @Override
    public String code() {
        return "realty.unit-sales-rate";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()                  { return "去化率"; }
            public String category()              { return "realty"; }
            public Set<String> applicableRoles()  { return Set.of("sales", "manager", "admin"); }
            public int defaultCols()              { return 4; }
            public int defaultRows()              { return 3; }
            public String suggestedChart()        { return "gauge"; }
            public String description()           { return "已售房源占总房源比例（SOLD+SIGNED+SUBSCRIBED / 总数）"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 总房源数
        long total = realtyUnitMapper.selectCount(new LambdaQueryWrapper<RealtyUnit>()
                .isNotNull(RealtyUnit::getStatus));

        // 已售：SOLD + SIGNED + SUBSCRIBED
        long sold = realtyUnitMapper.selectCount(new LambdaQueryWrapper<RealtyUnit>()
                .in(RealtyUnit::getStatus,
                        RealtyUnit.STATUS_SOLD,
                        RealtyUnit.STATUS_SIGNED,
                        RealtyUnit.STATUS_SUBSCRIBED));

        // 可售：AVAILABLE
        long available = realtyUnitMapper.selectCount(new LambdaQueryWrapper<RealtyUnit>()
                .eq(RealtyUnit::getStatus, RealtyUnit.STATUS_AVAILABLE));

        BigDecimal rate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(sold).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rate", rate);
        result.put("sold", sold);
        result.put("available", available);
        result.put("total", total);
        return result;
    }
}
