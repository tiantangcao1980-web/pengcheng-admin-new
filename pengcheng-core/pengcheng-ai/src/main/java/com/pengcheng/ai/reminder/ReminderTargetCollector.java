package com.pengcheng.ai.reminder;

import com.pengcheng.ai.reminder.entity.AiReminderRule;

import java.util.List;

/**
 * 提醒目标收集器（业务侧实现）。
 * <p>
 * AI 模块自身不耦合具体业务（客户/审批/公海池）查询逻辑——通过本端口由
 * 业务模块（pengcheng-realty / pengcheng-system）提供 Bean 注入，没有实现时
 * 默认走 {@link NoopReminderTargetCollector} 返回空列表，定时任务依旧可运行。
 */
public interface ReminderTargetCollector {

    /**
     * 是否处理该规则编码。
     */
    boolean supports(String ruleCode);

    /**
     * 收集需要推送的目标列表。
     */
    List<ReminderTarget> collect(AiReminderRule rule);
}
