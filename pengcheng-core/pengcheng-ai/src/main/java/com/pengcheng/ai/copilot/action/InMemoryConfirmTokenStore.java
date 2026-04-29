package com.pengcheng.ai.copilot.action;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ConfirmTokenStore} 的 InMemory 实现（单节点 MVP 足够用）。
 *
 * <p>特性：
 * <ul>
 *   <li>ConcurrentHashMap 保证基本并发安全（issue/consume 均为原子操作）</li>
 *   <li>TTL 默认 5 分钟，超时后 verifyAndConsume 返回 empty，同时惰性删除条目</li>
 *   <li>token 为 24 字节 SecureRandom → URL-safe Base64 无填充 = 32 字符</li>
 * </ul>
 *
 * <p>生产扩展：替换为 Redis 实现即可，接口不变。
 */
@Component
public class InMemoryConfirmTokenStore implements ConfirmTokenStore {

    /** token TTL（秒），默认 5 分钟 */
    static final long TTL_SECONDS = 5 * 60L;

    private final SecureRandom secureRandom = new SecureRandom();

    /** key = token，value = 缓存条目 */
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    @Override
    public String issue(CopilotActionProposal proposal, Long userId) {
        String token = generateToken();
        // 极低概率碰撞时重试一次
        cache.compute(token, (k, existing) -> {
            if (existing != null && !existing.isExpired()) {
                // 碰撞：重新生成一个 key 写入（put 到新 key）
                String newToken = generateToken();
                cache.put(newToken, new Entry(proposal, userId, Instant.now().plusSeconds(TTL_SECONDS)));
            }
            return new Entry(proposal, userId, Instant.now().plusSeconds(TTL_SECONDS));
        });
        return token;
    }

    @Override
    public Optional<CopilotActionProposal> verifyAndConsume(String token, Long userId) {
        if (token == null || userId == null) {
            return Optional.empty();
        }
        // 原子性地检查并移除，防止并发二次消费
        Entry[] found = new Entry[1];
        cache.compute(token, (k, entry) -> {
            if (entry == null || entry.isExpired()) {
                // 惰性清理过期条目
                found[0] = null;
                return null;
            }
            if (!userId.equals(entry.userId)) {
                // userId 不匹配：保留条目（不消费），防止猜测攻击
                found[0] = null;
                return entry;
            }
            // 校验通过：消费（返回 null 触发 compute 删除该 key）
            found[0] = entry;
            return null;
        });
        return Optional.ofNullable(found[0]).map(e -> e.proposal);
    }

    // -----------------------------------------------------------------------

    private String generateToken() {
        byte[] buf = new byte[24];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /** 缓存条目（不可变，线程安全）。 */
    private static final class Entry {
        final CopilotActionProposal proposal;
        final Long userId;
        final Instant expireAt;

        Entry(CopilotActionProposal proposal, Long userId, Instant expireAt) {
            this.proposal = proposal;
            this.userId = userId;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }
}
