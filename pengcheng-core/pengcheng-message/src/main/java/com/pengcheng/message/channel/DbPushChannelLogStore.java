package com.pengcheng.message.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * V4 MVP F4 — push_channel_log 真实落库实现。
 *
 * <p>替代 {@code V4MvpAutoConfiguration#defaultPushChannelLogStore} 的 Logging 兜底
 * （后者标注了 {@code @ConditionalOnMissingBean}，本类注册后自动让位）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>异步推送主链路不应被审计阻塞 — 任何 insert 异常都吞掉并打 WARN，
 *       避免推送主路径因日志落库失败而抛错；</li>
 *   <li>create_time 默认由 DB 填充（V55 表的 DEFAULT CURRENT_TIMESTAMP），
 *       这里仍补一份 LocalDateTime.now() 兜底；</li>
 *   <li>id 由 MyBatis-Plus 雪花算法分配（实体已 @TableId(IdType.ASSIGN_ID)）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbPushChannelLogStore implements PushChannelLogStore {

    private final PushChannelLogMapper mapper;

    @Override
    public void save(PushChannelLog logEntry) {
        if (logEntry == null) {
            return;
        }
        if (logEntry.getCreateTime() == null) {
            logEntry.setCreateTime(LocalDateTime.now());
        }
        try {
            mapper.insert(logEntry);
        } catch (Exception e) {
            // 审计日志落库失败不应影响推送主路径，只记录 WARN
            log.warn("[push_channel_log] 落库失败 userId={} channel={} success={} reason={}",
                    logEntry.getUserId(), logEntry.getChannel(),
                    logEntry.getSuccess(), logEntry.getReason(), e);
        }
    }
}
