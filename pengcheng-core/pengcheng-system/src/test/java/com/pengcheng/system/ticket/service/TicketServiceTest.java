package com.pengcheng.system.ticket.service;

import com.pengcheng.system.ticket.dto.TicketActionDTO;
import com.pengcheng.system.ticket.dto.TicketCreateDTO;
import com.pengcheng.system.ticket.entity.SysTicket;
import com.pengcheng.system.ticket.entity.SysTicketLog;
import com.pengcheng.system.ticket.enums.TicketStatus;
import com.pengcheng.system.ticket.mapper.SysTicketLogMapper;
import com.pengcheng.system.ticket.mapper.SysTicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TicketService 状态机单测
 */
@DisplayName("TicketService — 工单状态机")
class TicketServiceTest {

    private SysTicketMapper ticketMapper;
    private SysTicketLogMapper logMapper;
    private TicketService service;

    @BeforeEach
    void setUp() {
        ticketMapper = mock(SysTicketMapper.class);
        logMapper = mock(SysTicketLogMapper.class);
        service = new TicketService(ticketMapper, logMapper);
    }

    private SysTicket stub(long id, TicketStatus status) {
        SysTicket t = SysTicket.builder().status(status.name()).build();
        t.setId(id);
        when(ticketMapper.selectById(id)).thenReturn(t);
        return t;
    }

    // ========== create ==========

    @Test
    @DisplayName("创建：必填校验 — title")
    void create_missingTitle() {
        assertThatThrownBy(() -> service.create(TicketCreateDTO.builder()
                .category("IT").submitterId(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标题");
    }

    @Test
    @DisplayName("创建：必填校验 — category")
    void create_missingCategory() {
        assertThatThrownBy(() -> service.create(TicketCreateDTO.builder()
                .title("打印机坏了").submitterId(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("类型");
    }

    @Test
    @DisplayName("创建：必填校验 — submitterId")
    void create_missingSubmitter() {
        assertThatThrownBy(() -> service.create(TicketCreateDTO.builder()
                .title("打印机坏了").category("IT").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("提单人");
    }

    @Test
    @DisplayName("创建：状态置 CREATED + 写入日志 + 默认优先级 2")
    void create_happyPath() {
        service.create(TicketCreateDTO.builder()
                .title("打印机坏了").category("IT").submitterId(1L).build());

        ArgumentCaptor<SysTicket> tCap = ArgumentCaptor.forClass(SysTicket.class);
        verify(ticketMapper).insert(tCap.capture());
        assertThat(tCap.getValue().getStatus()).isEqualTo("CREATED");
        assertThat(tCap.getValue().getPriority()).isEqualTo(2);
        assertThat(tCap.getValue().getTicketNo()).startsWith("TKT-");

        ArgumentCaptor<SysTicketLog> lCap = ArgumentCaptor.forClass(SysTicketLog.class);
        verify(logMapper).insert(lCap.capture());
        assertThat(lCap.getValue().getAction()).isEqualTo("CREATE");
        assertThat(lCap.getValue().getToStatus()).isEqualTo("CREATED");
    }

    @Test
    @DisplayName("创建：优先级越界拒绝")
    void create_priorityOutOfRange() {
        assertThatThrownBy(() -> service.create(TicketCreateDTO.builder()
                .title("X").category("IT").submitterId(1L).priority(99).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("优先级");
    }

    // ========== assign ==========

    @Test
    @DisplayName("分配：CREATED → ASSIGNED 写入 assigneeId")
    void assign_succeeds() {
        SysTicket t = stub(100L, TicketStatus.CREATED);

        service.assign(TicketActionDTO.builder()
                .ticketId(100L).operatorId(2L).assigneeId(99L).build());

        assertThat(t.getStatus()).isEqualTo("ASSIGNED");
        assertThat(t.getAssigneeId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("分配：缺失 assigneeId 抛异常")
    void assign_missingAssignee() {
        assertThatThrownBy(() -> service.assign(TicketActionDTO.builder()
                .ticketId(100L).operatorId(2L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assignee");
    }

    @Test
    @DisplayName("分配：终态 CLOSED 不能再分配")
    void assign_closedRejected() {
        stub(100L, TicketStatus.CLOSED);

        assertThatThrownBy(() -> service.assign(TicketActionDTO.builder()
                .ticketId(100L).operatorId(2L).assigneeId(99L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
    }

    // ========== start / resolve / close ==========

    @Test
    @DisplayName("开始处理：ASSIGNED → IN_PROGRESS")
    void start_succeeds() {
        SysTicket t = stub(100L, TicketStatus.ASSIGNED);

        service.start(TicketActionDTO.builder().ticketId(100L).operatorId(99L).build());

        assertThat(t.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("解决：IN_PROGRESS → RESOLVED 写入 resolvedAt")
    void resolve_succeeds() {
        SysTicket t = stub(100L, TicketStatus.IN_PROGRESS);

        service.resolve(TicketActionDTO.builder()
                .ticketId(100L).operatorId(99L).content("已修好").build());

        assertThat(t.getStatus()).isEqualTo("RESOLVED");
        assertThat(t.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("关闭：RESOLVED → CLOSED 写入 closedAt")
    void close_succeeds() {
        SysTicket t = stub(100L, TicketStatus.RESOLVED);

        service.close(TicketActionDTO.builder()
                .ticketId(100L).operatorId(1L).build());

        assertThat(t.getStatus()).isEqualTo("CLOSED");
        assertThat(t.getClosedAt()).isNotNull();
    }

    // ========== cancel ==========

    @Test
    @DisplayName("取消：必须填写原因")
    void cancel_requiresReason() {
        stub(100L, TicketStatus.CREATED);

        assertThatThrownBy(() -> service.cancel(TicketActionDTO.builder()
                .ticketId(100L).operatorId(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原因");
    }

    @Test
    @DisplayName("取消：CREATED → CANCELLED 含原因")
    void cancel_succeeds() {
        SysTicket t = stub(100L, TicketStatus.CREATED);

        service.cancel(TicketActionDTO.builder()
                .ticketId(100L).operatorId(1L).content("重复提单").build());

        assertThat(t.getStatus()).isEqualTo("CANCELLED");
        assertThat(t.getClosedAt()).isNotNull();
    }

    // ========== reopen ==========

    @Test
    @DisplayName("重开：RESOLVED → IN_PROGRESS 清空 resolvedAt")
    void reopen_succeeds() {
        SysTicket t = stub(100L, TicketStatus.RESOLVED);
        t.setResolvedAt(java.time.LocalDateTime.now());

        service.reopen(TicketActionDTO.builder()
                .ticketId(100L).operatorId(1L).content("问题复现").build());

        assertThat(t.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(t.getResolvedAt()).isNull();
    }

    // ========== reply ==========

    @Test
    @DisplayName("回复：不改状态，仅写日志")
    void reply_noStatusChange() {
        SysTicket t = stub(100L, TicketStatus.IN_PROGRESS);

        service.reply(TicketActionDTO.builder()
                .ticketId(100L).operatorId(99L).content("已联系厂家").build());

        assertThat(t.getStatus()).isEqualTo("IN_PROGRESS");
        verify(ticketMapper, never()).updateById(any());
        verify(logMapper, times(1)).insert(any());
    }

    @Test
    @DisplayName("回复：内容为空抛异常")
    void reply_emptyContent() {
        assertThatThrownBy(() -> service.reply(TicketActionDTO.builder()
                .ticketId(100L).operatorId(99L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("内容");
    }

    // ========== 完整闭环 ==========

    @Test
    @DisplayName("完整闭环：CREATED → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED 写 5 条日志")
    void fullFlow() {
        // 用一个 stub ticket 跟踪状态变化
        SysTicket t = SysTicket.builder().status(TicketStatus.CREATED.name()).build();
        t.setId(100L);
        when(ticketMapper.selectById(100L)).thenReturn(t);

        service.assign(TicketActionDTO.builder().ticketId(100L).operatorId(1L).assigneeId(99L).build());
        service.start(TicketActionDTO.builder().ticketId(100L).operatorId(99L).build());
        service.resolve(TicketActionDTO.builder().ticketId(100L).operatorId(99L).content("ok").build());
        service.close(TicketActionDTO.builder().ticketId(100L).operatorId(1L).build());

        assertThat(t.getStatus()).isEqualTo("CLOSED");
        verify(logMapper, times(4)).insert(any());
    }
}
