package com.pengcheng.ai.reminder;

import com.pengcheng.ai.reminder.entity.AiReminderRule;

/**
 * 提醒推送出口端口。
 * <p>
 * 默认实现 {@link ChannelPushPortAdapter} 委托给 {@code ChannelPushService}；
 * 单测时可注入桩实现，便于断言"是否已被推送 / 推送参数"。
 */
public interface ReminderPushPort {

    /**
     * 推送一个提醒目标。
     *
     * @param rule   命中规则
     * @param target 渲染后的目标（含 title/content/userId）
     * @return 是否成功（仅代表入队，不代表对方一定收到）
     */
    boolean push(AiReminderRule rule, ReminderTarget target);
}
