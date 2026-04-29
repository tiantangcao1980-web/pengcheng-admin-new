package com.pengcheng.realty.dashboard.cards.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 我的待审批数卡片
 * <p>使用 PaymentRequest.status=1（待审批）作为待审批单据依据；
 * 当系统扩展通用审批流后可替换为 Workflow 审批表。
 */
@Component
@RequiredArgsConstructor
public class TeamApprovalPendingCardProvider implements DashboardCardProvider {

    private final PaymentRequestMapper paymentRequestMapper;

    @Override
    public String code() {
        return "team.approval.pending";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "待审批数"; }
            public String category()       { return "team"; }
            public Set<String> applicableRoles() { return Set.of("manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "当前用户名下待处理的付款审批单数量"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // status=1 待审批
        long pending = paymentRequestMapper.selectCount(new LambdaQueryWrapper<PaymentRequest>()
                .eq(PaymentRequest::getStatus, 1));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", pending);
        result.put("unit", "件");
        return result;
    }
}
