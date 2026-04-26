package com.pengcheng.ai.reminder;

import com.pengcheng.ai.reminder.entity.AiReminderRule;
import com.pengcheng.ai.reminder.mapper.AiReminderRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V4.0 D4 闭环④ 提醒规则引擎单测。
 * <p>
 * 覆盖：
 * <ul>
 *     <li>每日 9:00 提醒按 cron 触发，且同一天只触发一次（lastFiredAt 防抖）</li>
 *     <li>审批堆积 THRESHOLD 类型每次扫描都返回 true，由 collector 自身过滤</li>
 *     <li>模板渲染（${name}/${count}/${threshold}/${preDays}）</li>
 *     <li>fire() 调用 pushPort，并更新 lastFiredAt</li>
 * </ul>
 */
class ReminderRuleEngineTest {

    private AiReminderRuleMapper ruleMapper;
    private ReminderPushPort pushPort;
    private ReminderTargetCollector collector;
    private ReminderRuleEngine engine;

    @BeforeEach
    void setUp() {
        ruleMapper = mock(AiReminderRuleMapper.class);
        pushPort = mock(ReminderPushPort.class);
        collector = mock(ReminderTargetCollector.class);
        when(collector.supports(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        engine = new ReminderRuleEngine(ruleMapper, List.of(collector), pushPort);
    }

    @Test
    void dailyRule_shouldFireAtCronTime_andRespectSameDay() {
        AiReminderRule rule = newRule(AiReminderRule.Code.DAILY_FOLLOWUP, AiReminderRule.Type.DAILY,
                "0 0 9 * * ?", null, null);
        rule.setLastFiredAt(null);

        // 9:00:30 当天 → 应触发（cron 0 0 9 在过去 60s 窗口内）
        LocalDateTime nineAm = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0, 30));
        assertThat(engine.shouldFire(rule, nineAm)).isTrue();

        // 已触发过同一天 → 不再触发
        rule.setLastFiredAt(nineAm);
        assertThat(engine.shouldFire(rule, nineAm.plusMinutes(5))).isFalse();
    }

    @Test
    void dailyRule_shouldNotFireAtNonCronTime() {
        AiReminderRule rule = newRule(AiReminderRule.Code.DAILY_FOLLOWUP, AiReminderRule.Type.DAILY,
                "0 0 9 * * ?", null, null);
        // 14:00 不在 9:00 窗口
        LocalDateTime twoPm = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0, 0));
        assertThat(engine.shouldFire(rule, twoPm)).isFalse();
    }

    @Test
    void thresholdRule_shouldAlwaysReturnTrueWithinSchedule() {
        AiReminderRule rule = newRule(AiReminderRule.Code.APPROVAL_PENDING, AiReminderRule.Type.THRESHOLD,
                null, 120, null);
        assertThat(engine.shouldFire(rule, LocalDateTime.now())).isTrue();
    }

    @Test
    void preExpireRule_shouldAlwaysReturnTrueWithinSchedule() {
        AiReminderRule rule = newRule(AiReminderRule.Code.POOL_RECYCLE_PRE, AiReminderRule.Type.PRE_EXPIRE,
                null, null, 1);
        assertThat(engine.shouldFire(rule, LocalDateTime.now())).isTrue();
    }

    @Test
    void disabledRule_shouldNeverFire() {
        AiReminderRule rule = newRule(AiReminderRule.Code.DAILY_FOLLOWUP, AiReminderRule.Type.DAILY,
                "0 0 9 * * ?", null, null);
        rule.setEnabled(0);
        LocalDateTime nineAm = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0, 30));
        assertThat(engine.shouldFire(rule, nineAm)).isFalse();
    }

    @Test
    void invalidCron_shouldReturnFalseAndNotThrow() {
        AiReminderRule rule = newRule("X", AiReminderRule.Type.DAILY, "this is not a cron", null, null);
        assertThat(engine.shouldFire(rule, LocalDateTime.now())).isFalse();
    }

    @Test
    void renderTemplate_shouldSubstitutePlaceholders() {
        AiReminderRule rule = newRule(AiReminderRule.Code.APPROVAL_PENDING, AiReminderRule.Type.THRESHOLD,
                null, 120, null);
        rule.setTemplate("您有 ${count} 个审批超过 ${threshold} 分钟");

        ReminderTarget target = ReminderTarget.builder().userId(99L).count(7).build();
        ReminderTarget rendered = engine.renderTemplate(rule, target);

        assertThat(rendered.getContent()).isEqualTo("您有 7 个审批超过 120 分钟");
        assertThat(rendered.getTitle()).isEqualTo("审批堆积");
        assertThat(rendered.getRuleCode()).isEqualTo(AiReminderRule.Code.APPROVAL_PENDING);
    }

    @Test
    void fire_shouldPushTargetsAndUpdateLastFired() {
        AiReminderRule rule = newRule(AiReminderRule.Code.DAILY_FOLLOWUP, AiReminderRule.Type.DAILY,
                "0 0 9 * * ?", null, null);
        rule.setTemplate("您今日有 ${count} 个客户待跟进");
        List<ReminderTarget> targets = new ArrayList<>();
        targets.add(ReminderTarget.builder().userId(1L).count(3).name("张三").build());
        targets.add(ReminderTarget.builder().userId(2L).count(5).name("李四").build());
        when(collector.collect(rule)).thenReturn(targets);
        when(pushPort.push(org.mockito.ArgumentMatchers.eq(rule),
                org.mockito.ArgumentMatchers.any(ReminderTarget.class))).thenReturn(true);

        int sent = engine.fire(rule);

        assertThat(sent).isEqualTo(2);
        ArgumentCaptor<ReminderTarget> captor = ArgumentCaptor.forClass(ReminderTarget.class);
        verify(pushPort, atLeastOnce()).push(org.mockito.ArgumentMatchers.eq(rule), captor.capture());
        assertThat(captor.getAllValues()).extracting(ReminderTarget::getContent)
                .anyMatch(s -> s.contains("3 个"))
                .anyMatch(s -> s.contains("5 个"));
        assertThat(rule.getLastFiredAt()).isNotNull();
        verify(ruleMapper).updateById(rule);
    }

    @Test
    void fire_shouldHandleEmptyTargetsGracefully() {
        AiReminderRule rule = newRule(AiReminderRule.Code.POOL_RECYCLE_PRE, AiReminderRule.Type.PRE_EXPIRE,
                null, null, 1);
        when(collector.collect(rule)).thenReturn(Collections.emptyList());

        int sent = engine.fire(rule);

        assertThat(sent).isZero();
        verify(ruleMapper).updateById(rule);
    }

    private AiReminderRule newRule(String code, String type, String cron, Integer threshold, Integer preDays) {
        AiReminderRule rule = new AiReminderRule();
        rule.setId(1L);
        rule.setRuleCode(code);
        rule.setRuleName(type.equals(AiReminderRule.Type.THRESHOLD) ? "审批堆积" : "测试规则");
        rule.setRuleType(type);
        rule.setCronExpr(cron);
        rule.setThresholdMin(threshold);
        rule.setPreDays(preDays);
        rule.setEnabled(1);
        rule.setTemplate("default");
        return rule;
    }
}
