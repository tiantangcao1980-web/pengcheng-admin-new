package com.pengcheng.ai.reminder;

import com.pengcheng.ai.reminder.entity.AiReminderRule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;

/**
 * 默认空实现：当业务侧未提供具体的 {@link ReminderTargetCollector} Bean 时，
 * 注册该 Noop 实现避免空指针，同时打印一次提示日志（在 ReminderRuleEngine 中处理）。
 */
@Configuration
public class NoopReminderTargetCollector {

    @Bean
    @ConditionalOnMissingBean(ReminderTargetCollector.class)
    public ReminderTargetCollector noopReminderTargetCollector() {
        return new ReminderTargetCollector() {
            @Override
            public boolean supports(String ruleCode) {
                return true;
            }

            @Override
            public List<ReminderTarget> collect(AiReminderRule rule) {
                return Collections.emptyList();
            }
        };
    }
}
