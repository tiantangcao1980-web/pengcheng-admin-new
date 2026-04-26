package com.pengcheng.ai.copilot.action;

import com.pengcheng.ai.copilot.action.entity.AiCopilotActionLog;
import com.pengcheng.ai.copilot.action.mapper.AiCopilotActionLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V4.0 D4 闭环④ Copilot 动作服务单测。
 *
 * <p>覆盖：
 * <ul>
 *     <li>propose 生成 confirmToken + PENDING 状态</li>
 *     <li>confirm token 不匹配 → 抛异常</li>
 *     <li>confirm token TTL 过期 → 标记 CANCELLED 并抛异常</li>
 *     <li>confirm 正常 → 调用 executor → 状态变为 EXECUTED</li>
 *     <li>cancel 直接关闭 → CANCELLED</li>
 *     <li>没有 executor 时 stub 摘要兜底</li>
 * </ul>
 */
class CopilotActionServiceTest {

    private AiCopilotActionLogMapper mapper;
    private CopilotActionExecutor executor;
    private CopilotActionService service;

    /** 内存代替 mybatis：模拟 insert 自增 + selectById */
    private final Map<Long, AiCopilotActionLog> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        mapper = mock(AiCopilotActionLogMapper.class);
        when(mapper.insert(any(AiCopilotActionLog.class))).thenAnswer((InvocationOnMock i) -> {
            AiCopilotActionLog log = i.getArgument(0);
            log.setId(seq.getAndIncrement());
            store.put(log.getId(), log);
            return 1;
        });
        when(mapper.selectById(any())).thenAnswer((InvocationOnMock i) ->
                store.get(((Number) i.getArgument(0)).longValue()));
        when(mapper.updateById(any(AiCopilotActionLog.class))).thenAnswer((InvocationOnMock i) -> {
            AiCopilotActionLog log = i.getArgument(0);
            store.put(log.getId(), log);
            return 1;
        });

        executor = mock(CopilotActionExecutor.class);
        when(executor.supports(AiCopilotActionLog.Code.FOLLOW_UP_CREATE)).thenReturn(true);
        when(executor.execute(any())).thenReturn("已为客户王总新建跟进 #1001");

        service = new CopilotActionService(mapper, List.of(executor));
    }

    @Test
    void propose_shouldCreatePendingWithConfirmToken() {
        CopilotActionRequest req = baseRequest();
        CopilotActionProposal proposal = service.propose(req);

        assertThat(proposal.getActionId()).isNotNull();
        assertThat(proposal.getConfirmToken()).isNotBlank();
        assertThat(proposal.getStatus()).isEqualTo(AiCopilotActionLog.Status.PENDING);
        assertThat(proposal.getSummary()).contains("新建客户跟进");

        AiCopilotActionLog persisted = store.get(proposal.getActionId());
        assertThat(persisted.getStatus()).isEqualTo(AiCopilotActionLog.Status.PENDING);
        assertThat(persisted.getPayload()).contains("customerName");
    }

    @Test
    void propose_shouldRejectMissingActionCode() {
        CopilotActionRequest req = new CopilotActionRequest();
        req.setUserId(1L);
        assertThatThrownBy(() -> service.propose(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actionCode");
    }

    @Test
    void confirm_shouldRejectWrongToken() {
        CopilotActionProposal p = service.propose(baseRequest());
        assertThatThrownBy(() -> service.confirm(p.getActionId(), "WRONG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmToken");
    }

    @Test
    void confirm_shouldExecuteAndMarkExecuted() {
        CopilotActionProposal p = service.propose(baseRequest());
        String result = service.confirm(p.getActionId(), p.getConfirmToken());

        assertThat(result).contains("王总");
        AiCopilotActionLog log = store.get(p.getActionId());
        assertThat(log.getStatus()).isEqualTo(AiCopilotActionLog.Status.EXECUTED);
        assertThat(log.getExecutedAt()).isNotNull();
        assertThat(log.getResultSummary()).isEqualTo("已为客户王总新建跟进 #1001");
    }

    @Test
    void confirm_shouldBeIdempotentWhenAlreadyExecuted() {
        CopilotActionProposal p = service.propose(baseRequest());
        String first = service.confirm(p.getActionId(), p.getConfirmToken());
        String second = service.confirm(p.getActionId(), p.getConfirmToken());
        assertThat(second).isEqualTo(first);
    }

    @Test
    void confirm_shouldRejectExpiredToken() throws Exception {
        CopilotActionProposal p = service.propose(baseRequest());
        // 强行把 createTime 改成 11 分钟前 → 触发 TTL 过期
        AiCopilotActionLog log = store.get(p.getActionId());
        Field f = AiCopilotActionLog.class.getDeclaredField("createTime");
        f.setAccessible(true);
        f.set(log, LocalDateTime.now().minusMinutes(11));

        assertThatThrownBy(() -> service.confirm(p.getActionId(), p.getConfirmToken()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        assertThat(store.get(p.getActionId()).getStatus())
                .isEqualTo(AiCopilotActionLog.Status.CANCELLED);
    }

    @Test
    void cancel_shouldMarkCancelled() {
        CopilotActionProposal p = service.propose(baseRequest());
        service.cancel(p.getActionId(), p.getConfirmToken());
        assertThat(store.get(p.getActionId()).getStatus())
                .isEqualTo(AiCopilotActionLog.Status.CANCELLED);
    }

    @Test
    void cancel_shouldIgnoreWhenTokenMismatch() {
        CopilotActionProposal p = service.propose(baseRequest());
        service.cancel(p.getActionId(), "wrong-token");
        // 状态不变
        assertThat(store.get(p.getActionId()).getStatus())
                .isEqualTo(AiCopilotActionLog.Status.PENDING);
    }

    @Test
    void confirm_shouldFallbackToStubWhenNoExecutorMatches() {
        CopilotActionRequest req = baseRequest();
        req.setActionCode(AiCopilotActionLog.Code.TODO_CREATE);
        req.setPayload(Map.of("title", "回访王总"));
        CopilotActionProposal p = service.propose(req);

        // executor 只支持 FOLLOW_UP_CREATE，因此 TODO_CREATE 走 stub
        String result = service.confirm(p.getActionId(), p.getConfirmToken());
        assertThat(result).contains("[stub]").contains(AiCopilotActionLog.Code.TODO_CREATE);
        assertThat(store.get(p.getActionId()).getStatus())
                .isEqualTo(AiCopilotActionLog.Status.EXECUTED);
    }

    private CopilotActionRequest baseRequest() {
        CopilotActionRequest req = new CopilotActionRequest();
        req.setActionCode(AiCopilotActionLog.Code.FOLLOW_UP_CREATE);
        req.setConversationId("conv-1");
        req.setUserId(42L);
        req.setPagePath("/customer/detail");
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", "王总");
        payload.put("note", "上午来电咨询学区房");
        req.setPayload(payload);
        return req;
    }
}
