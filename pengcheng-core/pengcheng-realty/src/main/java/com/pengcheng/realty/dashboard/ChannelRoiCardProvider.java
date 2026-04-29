package com.pengcheng.realty.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 渠道 ROI 卡片：按联盟商统计带看数/成交数/佣金成本/销售额，计算 ROI，Top 10 倒序
 *
 * <p>ROI 公式：(销售额 - 佣金成本) / 佣金成本，佣金成本为 0 时显示 null
 *
 * <p>数据关联：
 * <ul>
 *   <li>带看数：customer.alliance_id GROUP BY，visitTime 在时间窗内</li>
 *   <li>成交数：customer_deal（sign_status=1）JOIN customer.alliance_id，dealTime 在时间窗内</li>
 *   <li>佣金成本：commission.payable_amount SUM by alliance_id，auditTime 在时间窗内</li>
 *   <li>销售额：customer_deal.deal_amount SUM by customer.alliance_id，dealTime 在时间窗内</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ChannelRoiCardProvider implements DashboardCardProvider {

    private final AllianceMapper allianceMapper;
    private final RealtyCustomerMapper customerMapper;
    private final CustomerDealMapper customerDealMapper;
    private final CommissionMapper commissionMapper;

    @Override
    public String code() {
        return "realty.channel-roi";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()                  { return "渠道 ROI"; }
            public String category()              { return "realty"; }
            public Set<String> applicableRoles()  { return Set.of("manager", "admin"); }
            public int defaultCols()              { return 8; }
            public int defaultRows()              { return 4; }
            public String suggestedChart()        { return "table"; }
            public String description()           { return "各渠道联盟商带看数/成交数/佣金成本/销售额/ROI，按 ROI 倒序 Top 10"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 1. 所有启用的联盟商
        List<Alliance> alliances = allianceMapper.selectList(
                new LambdaQueryWrapper<Alliance>().eq(Alliance::getStatus, 1));

        // 2. 时间窗内的客户（带看人数）按 alliance_id 分组
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .isNotNull(Customer::getAllianceId)
                .ge(ctx.windowStart() != null, Customer::getVisitTime, ctx.windowStart())
                .le(ctx.windowEnd() != null, Customer::getVisitTime, ctx.windowEnd()));
        Map<Long, Long> visitsByAlliance = customers.stream()
                .collect(Collectors.groupingBy(Customer::getAllianceId, Collectors.counting()));

        // 3. 时间窗内已签约成交记录，通过 customerId -> customer.allianceId 映射
        List<CustomerDeal> signedDeals = customerDealMapper.selectList(new LambdaQueryWrapper<CustomerDeal>()
                .eq(CustomerDeal::getSignStatus, 1)
                .ge(ctx.windowStart() != null, CustomerDeal::getDealTime, ctx.windowStart())
                .le(ctx.windowEnd() != null, CustomerDeal::getDealTime, ctx.windowEnd()));
        // 取涉及的 customerId 集合，再批量查 customer.alliance_id
        Set<Long> dealCustomerIds = signedDeals.stream()
                .map(CustomerDeal::getCustomerId).collect(Collectors.toSet());
        Map<Long, Long> customerAllianceMap = dealCustomerIds.isEmpty()
                ? Map.of()
                : customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                        .in(Customer::getId, dealCustomerIds))
                .stream().filter(c -> c.getAllianceId() != null)
                .collect(Collectors.toMap(Customer::getId, Customer::getAllianceId, (a, b) -> a));

        // 成交数 by alliance
        Map<Long, Long> dealsByAlliance = signedDeals.stream()
                .filter(d -> customerAllianceMap.containsKey(d.getCustomerId()))
                .collect(Collectors.groupingBy(
                        d -> customerAllianceMap.get(d.getCustomerId()), Collectors.counting()));

        // 销售额 by alliance
        Map<Long, BigDecimal> salesByAlliance = signedDeals.stream()
                .filter(d -> customerAllianceMap.containsKey(d.getCustomerId()) && d.getDealAmount() != null)
                .collect(Collectors.groupingBy(
                        d -> customerAllianceMap.get(d.getCustomerId()),
                        Collectors.reducing(BigDecimal.ZERO, CustomerDeal::getDealAmount, BigDecimal::add)));

        // 4. 佣金成本（payable_amount SUM）by alliance_id
        List<Commission> commissions = commissionMapper.selectList(new LambdaQueryWrapper<Commission>()
                .isNotNull(Commission::getAllianceId)
                .ge(ctx.windowStart() != null, Commission::getAuditTime, ctx.windowStart())
                .le(ctx.windowEnd() != null, Commission::getAuditTime, ctx.windowEnd()));
        Map<Long, BigDecimal> commissionByAlliance = commissions.stream()
                .filter(c -> c.getPayableAmount() != null)
                .collect(Collectors.groupingBy(Commission::getAllianceId,
                        Collectors.reducing(BigDecimal.ZERO, Commission::getPayableAmount, BigDecimal::add)));

        // 5. 组装 ROI 行
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Alliance a : alliances) {
            Long aid = a.getId();
            long visits = visitsByAlliance.getOrDefault(aid, 0L);
            long deals = dealsByAlliance.getOrDefault(aid, 0L);
            BigDecimal commCost = commissionByAlliance.getOrDefault(aid, BigDecimal.ZERO);
            BigDecimal sales = salesByAlliance.getOrDefault(aid, BigDecimal.ZERO);

            // 只包含有带看或成交数据的渠道
            if (visits == 0 && deals == 0) continue;

            BigDecimal roi = null;
            if (commCost.compareTo(BigDecimal.ZERO) > 0) {
                roi = sales.subtract(commCost)
                        .divide(commCost, 4, RoundingMode.HALF_UP);
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("allianceName", a.getCompanyName());
            row.put("visits", visits);
            row.put("deals", deals);
            row.put("commissionCost", commCost);
            row.put("salesAmount", sales);
            row.put("roi", roi);
            rows.add(row);
        }

        // 6. 按 ROI 倒序，null 排最后，取 Top 10
        rows.sort(Comparator.comparing(
                (Map<String, Object> r) -> (BigDecimal) r.get("roi"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<Map<String, Object>> top10 = rows.size() > 10 ? rows.subList(0, 10) : rows;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", top10);
        return result;
    }
}
