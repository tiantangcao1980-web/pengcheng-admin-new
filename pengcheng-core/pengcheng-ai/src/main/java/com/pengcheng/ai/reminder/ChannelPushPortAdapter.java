package com.pengcheng.ai.reminder;

import com.pengcheng.ai.reminder.entity.AiReminderRule;
import com.pengcheng.system.channel.service.ChannelPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认推送适配器：尝试通过 {@link ChannelPushService} 推送到外部渠道（钉钉/飞书/企业微信）。
 * <p>
 * 当 D5 模块（统一三通道调度）尚未到位时，仅广播到当前已配置的群聊渠道；
 * 没有配置任何渠道也不会抛异常，会以日志兜底，以便定时任务持续运行。
 * <p>
 * 单测注入 mock {@link ChannelPushService} 即可断言。
 */
@Slf4j
@Component
public class ChannelPushPortAdapter implements ReminderPushPort {

    private final ChannelPushService channelPushService;

    @Autowired
    public ChannelPushPortAdapter(ChannelPushService channelPushService) {
        this.channelPushService = channelPushService;
    }

    @Override
    public boolean push(AiReminderRule rule, ReminderTarget target) {
        if (target == null || target.getContent() == null || target.getContent().isBlank()) {
            return false;
        }
        try {
            if (channelPushService != null) {
                channelPushService.broadcast(
                        target.getTitle() != null ? target.getTitle() : rule.getRuleName(),
                        target.getContent(),
                        "ai_reminder:" + rule.getRuleCode());
            } else {
                log.info("[ai-reminder] (no ChannelPushService) rule={} userId={} content={}",
                        rule.getRuleCode(), target.getUserId(), target.getContent());
            }
            return true;
        } catch (Exception e) {
            // 推送失败不影响调度循环；与现有 ChannelPushService 内部捕获保持一致
            log.warn("[ai-reminder] push failed rule={} userId={} err={}",
                    rule.getRuleCode(), target.getUserId(), e.getMessage());
            return false;
        }
    }
}
