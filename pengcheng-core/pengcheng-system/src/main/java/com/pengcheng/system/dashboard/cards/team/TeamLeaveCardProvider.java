package com.pengcheng.system.dashboard.cards.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.attendance.entity.LeaveRequest;
import com.pengcheng.hr.attendance.mapper.LeaveRequestMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本月请假人天卡片
 * <p>status=1 表示已审批通过的请假申请；人天 = sum( DATEDIFF(endTime, startTime) + 1 )
 */
@Component
@RequiredArgsConstructor
public class TeamLeaveCardProvider implements DashboardCardProvider {

    private final LeaveRequestMapper leaveRequestMapper;

    @Override
    public String code() {
        return "team.leave";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "本月请假人天"; }
            public String category()       { return "team"; }
            public Set<String> applicableRoles() { return Set.of("manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "本月已审批通过的请假申请累计人天数"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        // 已通过(status=1)的请假
        List<LeaveRequest> approved = leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStatus, 1)
                .ge(LeaveRequest::getStartTime, monthStart)
                .le(LeaveRequest::getStartTime, monthEnd));

        long totalDays = approved.stream()
                .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                .mapToLong(r -> {
                    long days = ChronoUnit.DAYS.between(r.getStartTime().toLocalDate(), r.getEndTime().toLocalDate()) + 1;
                    return Math.max(0, days);
                })
                .sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", totalDays);
        result.put("unit", "人天");
        result.put("count", approved.size());
        return result;
    }
}
