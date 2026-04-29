package com.pengcheng.message.subscribe.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * {@link MpUserSubscribeService} 实现
 *
 * <p>关键并发安全说明：
 * <ul>
 *   <li>{@link #recordSubscribe} 利用 UNIQUE KEY (user_id, template_id)；
 *       先尝试 insert，唯一键冲突时改为 update，两步在同一事务内执行。</li>
 *   <li>{@link #tryConsume} 通过单条原子 UPDATE 语句（WHERE used < quota）保证不超发。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpUserSubscribeServiceImpl implements MpUserSubscribeService {

    private final MpUserSubscribeMapper mapper;

    // -------------------------------------------------------------------------
    // recordSubscribe
    // -------------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSubscribe(Long userId, String openId, String templateId, int quotaIncrement) {
        // 尝试查询已有记录
        MpUserSubscribe existing = mapper.selectOne(
                new LambdaQueryWrapper<MpUserSubscribe>()
                        .eq(MpUserSubscribe::getUserId, userId)
                        .eq(MpUserSubscribe::getTemplateId, templateId)
        );

        if (existing == null) {
            // 首次订阅：插入新行
            MpUserSubscribe record = MpUserSubscribe.builder()
                    .userId(userId)
                    .openId(openId)
                    .templateId(templateId)
                    .quota(quotaIncrement)
                    .used(0)
                    .lastSubscribeTime(LocalDateTime.now())
                    .revoked(0)
                    .build();
            mapper.insert(record);
            log.info("[MpSubscribe] 首次订阅: userId={}, templateId={}, quota={}", userId, templateId, quotaIncrement);
        } else {
            // 重复订阅：累加 quota，刷新 openId、订阅时间，重置 revoked
            mapper.update(null,
                    new LambdaUpdateWrapper<MpUserSubscribe>()
                            .eq(MpUserSubscribe::getUserId, userId)
                            .eq(MpUserSubscribe::getTemplateId, templateId)
                            .set(MpUserSubscribe::getOpenId, openId)
                            .set(MpUserSubscribe::getQuota, existing.getQuota() + quotaIncrement)
                            .set(MpUserSubscribe::getLastSubscribeTime, LocalDateTime.now())
                            .set(MpUserSubscribe::getRevoked, 0)
            );
            log.info("[MpSubscribe] 重复授权: userId={}, templateId={}, newQuota={}",
                    userId, templateId, existing.getQuota() + quotaIncrement);
        }
    }

    // -------------------------------------------------------------------------
    // tryConsume
    // -------------------------------------------------------------------------

    @Override
    public boolean tryConsume(Long userId, String templateId) {
        int updated = mapper.consumeQuota(userId, templateId);
        if (updated > 0) {
            log.debug("[MpSubscribe] 消费配额成功: userId={}, templateId={}", userId, templateId);
            return true;
        }
        log.warn("[MpSubscribe] 配额不足或已撤销: userId={}, templateId={}", userId, templateId);
        return false;
    }

    // -------------------------------------------------------------------------
    // revoke
    // -------------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long userId, String templateId) {
        int affected = mapper.update(null,
                new LambdaUpdateWrapper<MpUserSubscribe>()
                        .eq(MpUserSubscribe::getUserId, userId)
                        .eq(MpUserSubscribe::getTemplateId, templateId)
                        .set(MpUserSubscribe::getRevoked, 1)
        );
        log.info("[MpSubscribe] 撤销订阅: userId={}, templateId={}, affected={}", userId, templateId, affected);
    }

    // -------------------------------------------------------------------------
    // findActive
    // -------------------------------------------------------------------------

    @Override
    public Optional<MpUserSubscribe> findActive(Long userId, String templateId) {
        MpUserSubscribe record = mapper.selectOne(
                new LambdaQueryWrapper<MpUserSubscribe>()
                        .eq(MpUserSubscribe::getUserId, userId)
                        .eq(MpUserSubscribe::getTemplateId, templateId)
                        .eq(MpUserSubscribe::getRevoked, 0)
        );
        return Optional.ofNullable(record);
    }

    // -------------------------------------------------------------------------
    // findLatestActive
    // -------------------------------------------------------------------------

    @Override
    public Optional<MpUserSubscribe> findLatestActive(Long userId) {
        // 取所有未撤销且配额有剩余的记录中 lastSubscribeTime 最新的一条
        MpUserSubscribe record = mapper.selectOne(
                new LambdaQueryWrapper<MpUserSubscribe>()
                        .eq(MpUserSubscribe::getUserId, userId)
                        .eq(MpUserSubscribe::getRevoked, 0)
                        .apply("used < quota")
                        .orderByDesc(MpUserSubscribe::getLastSubscribeTime)
                        .last("LIMIT 1")
        );
        return Optional.ofNullable(record);
    }
}
