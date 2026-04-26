package com.pengcheng.ai.reminder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.ai.reminder.entity.AiReminderRule;
import com.pengcheng.ai.reminder.mapper.AiReminderRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 提醒规则执行引擎（V4.0 MVP 闭环④）。
 * <p>
 * 三种类型的触发判断与目标渲染统一在这里完成；具体定时调度由
 * {@link ReminderScheduler} 负责。
 * <p>
 * 已与具体业务（客户/审批/公海池）解耦：通过 {@link ReminderTargetCollector}
 * 列表收集目标，由业务侧（realty / system）提供 Bean。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderRuleEngine {

    private final AiReminderRuleMapper ruleMapper;
    private final List<ReminderTargetCollector> collectors;
    private final ReminderPushPort pushPort;

    /**
     * 列出全部启用规则（供 Scheduler / 单测使用）。
     */
    public List<AiReminderRule> listEnabledRules() {
        return ruleMapper.selectList(
                new LambdaQueryWrapper<AiReminderRule>().eq(AiReminderRule::getEnabled, 1));
    }

    /**
     * 判断指定规则在 {@code now} 时刻是否应触发（结合 lastFiredAt 防抖）。
     *
     * @param rule 规则
     * @param now  当前时间
     * @return true 表示应触发
     */
    public boolean shouldFire(AiReminderRule rule, LocalDateTime now) {
        if (rule == null || !rule.isEnabled()) {
            return false;
        }
        switch (Objects.requireNonNullElse(rule.getRuleType(), "")) {
            case AiReminderRule.Type.DAILY:
                return shouldFireDaily(rule, now);
            case AiReminderRule.Type.THRESHOLD:
            case AiReminderRule.Type.PRE_EXPIRE:
                // 这两类每次扫描周期都检查，由业务侧 Collector 自行过滤"未达阈值/未到期"目标
                return true;
            default:
                return false;
        }
    }

    private boolean shouldFireDaily(AiReminderRule rule, LocalDateTime now) {
        if (rule.getCronExpr() == null || rule.getCronExpr().isBlank()) {
            return false;
        }
        LocalDateTime last = rule.getLastFiredAt();
        // 同一天已触发过 → 跳过（避免重复推送）
        if (last != null && last.toLocalDate().isEqual(now.toLocalDate())) {
            return false;
        }
        try {
            CronExpression expr = CronExpression.parse(rule.getCronExpr());
            // 在过去 60 秒窗口内是否有 cron 触发点
            LocalDateTime probe = now.minusSeconds(60);
            LocalDateTime next = expr.next(probe.atZone(ZoneId.systemDefault()))
                    == null ? null
                    : expr.next(probe.atZone(ZoneId.systemDefault()))
                              .toLocalDateTime();
            return next != null && (next.isBefore(now) || next.isEqual(now));
        } catch (IllegalArgumentException e) {
            log.warn("[ai-reminder] invalid cron expr rule={} expr={} err={}",
                    rule.getRuleCode(), rule.getCronExpr(), e.getMessage());
            return false;
        }
    }

    /**
     * 执行单条规则：收集目标 → 渲染模板 → 推送 → 更新 last_fired_at。
     *
     * @return 实际推送数量
     */
    public int fire(AiReminderRule rule) {
        if (rule == null) {
            return 0;
        }
        List<ReminderTarget> targets = collectTargets(rule);
        int sent = 0;
        for (ReminderTarget target : targets) {
            ReminderTarget rendered = renderTemplate(rule, target);
            if (pushPort.push(rule, rendered)) {
                sent++;
            }
        }
        rule.setLastFiredAt(LocalDateTime.now());
        ruleMapper.updateById(rule);
        log.info("[ai-reminder] fire rule={} sent={}", rule.getRuleCode(), sent);
        return sent;
    }

    /**
     * 渲染模板（支持 ${name} ${count} ${threshold} ${preDays} 占位符）。
     */
    public ReminderTarget renderTemplate(AiReminderRule rule, ReminderTarget target) {
        if (target == null) {
            return null;
        }
        String tpl = rule.getTemplate() != null ? rule.getTemplate() : "";
        String content = tpl
                .replace("${name}", target.getName() != null ? target.getName() : "")
                .replace("${count}", target.getCount() != null ? String.valueOf(target.getCount()) : "0")
                .replace("${threshold}", rule.getThresholdMin() != null ? String.valueOf(rule.getThresholdMin()) : "")
                .replace("${preDays}", rule.getPreDays() != null ? String.valueOf(rule.getPreDays()) : "");
        target.setContent(content);
        if (target.getTitle() == null) {
            target.setTitle(rule.getRuleName());
        }
        target.setRuleCode(rule.getRuleCode());
        return target;
    }

    private List<ReminderTarget> collectTargets(AiReminderRule rule) {
        for (ReminderTargetCollector collector : collectors) {
            if (collector.supports(rule.getRuleCode())) {
                List<ReminderTarget> result = collector.collect(rule);
                return result != null ? result : Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
