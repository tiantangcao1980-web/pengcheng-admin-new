package com.pengcheng.push.unified;

import lombok.extern.slf4j.Slf4j;

/**
 * 站内信兜底实现（NotificationService 不在 classpath 时使用）。
 */
@Slf4j
public class NoOpChannelInboxSender implements ChannelInboxSender {

    @Override
    public boolean send(String userId, String title, String content, String bizType, Long bizId) {
        log.warn("[NoOpInboxSender] 站内信被丢弃（无真实 sender）：userId={}, title={}", userId, title);
        return false;
    }
}
