package com.pengcheng.system.dashboard.cards.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.performance.entity.KpiScore;
import com.pengcheng.hr.performance.mapper.KpiScoreMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本月团队目标完成度仪表盘卡片
 * <p>从 KpiScore 取当前周期加权得分 / 目标值 得出完成比率。
 * KpiScore 无时间字段，取最新 periodId（最大 ID 近似当前周期）。
 * TODO: 待 Phase 5 确认 KpiPeriod 关联当月查询方式后精化
 */
@Component
@RequiredArgsConstructor
public class TeamGoalProgressCardProvider implements DashboardCardProvider {

    private final KpiScoreMapper kpiScoreMapper;

    @Override
    public String code() {
        return "team.goal";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "团队目标完成度"; }
            public String category()       { return "team"; }
            public Set<String> applicableRoles() { return Set.of("manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 3; }
            public String suggestedChart() { return "gauge"; }
            public String description()    { return "本月团队 KPI 目标完成进度（加权得分 / 目标值）"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        // 取最新 periodId（近似本期）
        KpiScore latest = kpiScoreMapper.selectOne(new LambdaQueryWrapper<KpiScore>()
                .orderByDesc(KpiScore::getPeriodId)
                .last("LIMIT 1"));

        double progress = 0.0;
        if (latest != null && latest.getPeriodId() != null) {
            List<KpiScore> periodScores = kpiScoreMapper.selectList(new LambdaQueryWrapper<KpiScore>()
                    .eq(KpiScore::getPeriodId, latest.getPeriodId())
                    .isNotNull(KpiScore::getWeightedScore)
                    .isNotNull(KpiScore::getTargetValue));

            BigDecimal totalWeighted = periodScores.stream()
                    .map(KpiScore::getWeightedScore)
                    .filter(s -> s != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalTarget = periodScores.stream()
                    .map(KpiScore::getTargetValue)
                    .filter(t -> t != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
                progress = totalWeighted.divide(totalTarget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", Math.min(progress, 100.0));
        result.put("unit", "%");
        return result;
    }
}
