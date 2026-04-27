package com.pengcheng.system.dashboard.cards.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 销售排行榜 Top 10 卡片（按本月成交金额）
 * <p>TODO: customer_deal 缺乏 creator_id/sales_user_id 字段，当前以 BaseEntity.createBy 作为销售人员标识；
 * Phase 5 销售人员字段完善后可精确关联。
 */
@Component
@RequiredArgsConstructor
public class TeamSalesRankCardProvider implements DashboardCardProvider {

    private final CustomerDealMapper customerDealMapper;

    @Override
    public String code() {
        return "team.sales.rank";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "销售排行榜"; }
            public String category()       { return "team"; }
            public Set<String> applicableRoles() { return Set.of("manager", "admin"); }
            public int defaultCols()       { return 6; }
            public int defaultRows()       { return 4; }
            public String suggestedChart() { return "bar"; }
            public String description()    { return "本月销售人员按成交金额 Top 10 排名"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        List<CustomerDeal> deals = customerDealMapper.selectList(new LambdaQueryWrapper<CustomerDeal>()
                .ge(CustomerDeal::getDealTime, monthStart)
                .le(CustomerDeal::getDealTime, monthEnd)
                .isNotNull(CustomerDeal::getDealAmount));

        // 按 createBy(userId) 聚合成交金额
        Map<String, BigDecimal> byUser = new LinkedHashMap<>();
        for (CustomerDeal d : deals) {
            String user = d.getCreateBy() != null ? d.getCreateBy().toString() : "未知";
            byUser.merge(user, d.getDealAmount() != null ? d.getDealAmount() : BigDecimal.ZERO, BigDecimal::add);
        }

        // 排序取 Top10
        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(byUser.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            xAxis.add(sorted.get(i).getKey());
            values.add(sorted.get(i).getValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("xAxis", xAxis);
        result.put("series", List.of(Map.of("name", "成交金额", "data", values)));
        return result;
    }
}
