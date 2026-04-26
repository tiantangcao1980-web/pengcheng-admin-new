package com.pengcheng.ai.reminder;

import com.pengcheng.ai.reminder.entity.AiReminderRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI 提醒定时调度 Bean（仅在 ai 模块内）。
 * <p>
 * 红线：不动 pengcheng-job 的 SysJob/SysJobLog；仅在本模块用 Spring 调度。
 * 默认每分钟扫描一次启用规则；通过 {@code pengcheng.ai.reminder.scheduler-enabled=false}
 * 可禁用（测试或临时停机用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pengcheng.ai.reminder.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private final ReminderRuleEngine engine;

    /** 每分钟整点执行一次扫描 */
    @Scheduled(cron = "0 * * * * ?")
    public void scan() {
        LocalDateTime now = LocalDateTime.now();
        for (AiReminderRule rule : engine.listEnabledRules()) {
            try {
                if (engine.shouldFire(rule, now)) {
                    engine.fire(rule);
                }
            } catch (Exception e) {
                log.warn("[ai-reminder] schedule fire failed rule={} err={}", rule.getRuleCode(), e.getMessage());
            }
        }
    }
}
