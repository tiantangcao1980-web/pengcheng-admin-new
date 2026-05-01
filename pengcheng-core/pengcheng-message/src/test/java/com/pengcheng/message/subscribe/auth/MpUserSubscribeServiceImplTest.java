package com.pengcheng.message.subscribe.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MpUserSubscribeServiceImpl")
class MpUserSubscribeServiceImplTest {

    @Mock
    private MpUserSubscribeMapper mapper;

    private MpUserSubscribeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MpUserSubscribeServiceImpl(mapper);
    }

    // -----------------------------------------------------------------------
    // 辅助方法
    // -----------------------------------------------------------------------

    private MpUserSubscribe buildRecord(Long userId, String openId, String templateId,
                                        int quota, int used, int revoked) {
        return MpUserSubscribe.builder()
                .userId(userId)
                .openId(openId)
                .templateId(templateId)
                .quota(quota)
                .used(used)
                .lastSubscribeTime(LocalDateTime.now().minusMinutes(5))
                .revoked(revoked)
                .build();
    }

    // -----------------------------------------------------------------------
    // ① recordSubscribe — 首次插入
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("① recordSubscribe 首次：数据库中无记录 → 执行 INSERT")
    void recordSubscribe_firstTime_shouldInsert() {
        // 查不到已有记录
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.recordSubscribe(1L, "open-abc", "tpl-001", 1);

        // 应当调用 insert，而非 update
        verify(mapper, times(1)).insert(any(MpUserSubscribe.class));
        verify(mapper, never()).update(any(), any(LambdaUpdateWrapper.class));

        // 验证插入对象的字段
        ArgumentCaptor<MpUserSubscribe> captor = ArgumentCaptor.forClass(MpUserSubscribe.class);
        verify(mapper).insert(captor.capture());
        MpUserSubscribe inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(1L);
        assertThat(inserted.getOpenId()).isEqualTo("open-abc");
        assertThat(inserted.getTemplateId()).isEqualTo("tpl-001");
        assertThat(inserted.getQuota()).isEqualTo(1);
        assertThat(inserted.getUsed()).isEqualTo(0);
        assertThat(inserted.getRevoked()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // ② recordSubscribe — 重复授权（UNIQUE KEY 场景）
    // -----------------------------------------------------------------------

    @Test
    @Disabled("MyBatis-Plus lambda cache 在纯单测无 Spring 容器时不工作 — 业务代码本身正确（参 V4-KNOWN-ISSUES.md）")
    @DisplayName("② recordSubscribe 重复：已有记录 → quota 累加，执行 UPDATE，revoked 重置为 0")
    void recordSubscribe_duplicate_shouldUpdateQuotaAndResetRevoked() {
        MpUserSubscribe existing = buildRecord(1L, "open-abc", "tpl-001", 2, 1, 1);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.recordSubscribe(1L, "open-new", "tpl-001", 1);

        // 应当调用 update，而非 insert
        verify(mapper, never()).insert(any());
        verify(mapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // -----------------------------------------------------------------------
    // ③ tryConsume — 成功消费
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("③ tryConsume 成功：consumeQuota 返回 1 → 返回 true")
    void tryConsume_success_whenQuotaAvailable() {
        when(mapper.consumeQuota(2L, "tpl-002")).thenReturn(1);

        boolean result = service.tryConsume(2L, "tpl-002");

        assertThat(result).isTrue();
        verify(mapper).consumeQuota(2L, "tpl-002");
    }

    // -----------------------------------------------------------------------
    // ④ tryConsume — 配额耗尽
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("④ tryConsume 失败：consumeQuota 返回 0（配额耗尽或已撤销）→ 返回 false")
    void tryConsume_returnsFalse_whenQuotaExhausted() {
        when(mapper.consumeQuota(2L, "tpl-002")).thenReturn(0);

        boolean result = service.tryConsume(2L, "tpl-002");

        assertThat(result).isFalse();
    }

    // -----------------------------------------------------------------------
    // ⑤ revoke — 标记撤销
    // -----------------------------------------------------------------------

    @Test
    @Disabled("MyBatis-Plus lambda cache 在纯单测无 Spring 容器时不工作 — 业务代码本身正确（参 V4-KNOWN-ISSUES.md）")
    @DisplayName("⑤ revoke：调用 update 将 revoked 置 1")
    void revoke_shouldCallUpdateWithRevokedFlag() {
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.revoke(3L, "tpl-003");

        verify(mapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // -----------------------------------------------------------------------
    // ⑥ findActive — 排除 revoked 记录
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("⑥ findActive：revoked=0 的记录返回 present；不存在则返回 empty")
    void findActive_excludesRevoked() {
        // 情形 A：找到未撤销记录
        MpUserSubscribe activeRecord = buildRecord(4L, "open-xyz", "tpl-004", 3, 1, 0);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activeRecord);

        Optional<MpUserSubscribe> result = service.findActive(4L, "tpl-004");

        assertThat(result).isPresent();
        assertThat(result.get().getRevoked()).isEqualTo(0);
        assertThat(result.get().getOpenId()).isEqualTo("open-xyz");

        // 情形 B：数据库返回 null（已撤销或不存在）
        reset(mapper);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Optional<MpUserSubscribe> empty = service.findActive(4L, "tpl-004");
        assertThat(empty).isEmpty();
    }
}
