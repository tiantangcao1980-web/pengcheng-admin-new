package com.pengcheng.system.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.ticket.dto.TicketActionDTO;
import com.pengcheng.system.ticket.dto.TicketCreateDTO;
import com.pengcheng.system.ticket.entity.SysTicket;
import com.pengcheng.system.ticket.entity.SysTicketLog;
import com.pengcheng.system.ticket.enums.TicketStatus;
import com.pengcheng.system.ticket.mapper.SysTicketLogMapper;
import com.pengcheng.system.ticket.mapper.SysTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 轻量工单服务（IT/HR 报修等内部流转）
 *
 * 状态机由 TicketStatus 枚举集中管理，所有流转动作走 transition()。
 * 每次状态变更写一条 sys_ticket_log，保证审计可追溯。
 *
 * 不引入 Flowable / 不复用 automation：
 *   - automation 是规则触发型（事件 → 动作），不适合线性状态机
 *   - Flowable 是重型 BPMN，V2.0 工单升级时再引入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final SysTicketMapper ticketMapper;
    private final SysTicketLogMapper logMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================================================
    // 创建
    // ============================================================

    @Transactional
    public Long create(TicketCreateDTO dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new IllegalArgumentException("类型不能为空");
        }
        if (dto.getSubmitterId() == null) {
            throw new IllegalArgumentException("提单人不能为空");
        }

        int priority = dto.getPriority() == null ? SysTicket.PRIORITY_MEDIUM : dto.getPriority();
        if (priority < 1 || priority > 4) {
            throw new IllegalArgumentException("优先级取值 1-4");
        }

        SysTicket ticket = SysTicket.builder()
                .ticketNo(generateTicketNo())
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .priority(priority)
                .status(TicketStatus.CREATED.name())
                .submitterId(dto.getSubmitterId())
                .build();
        ticketMapper.insert(ticket);

        writeLog(ticket.getId(), SysTicketLog.ACTION_CREATE, null,
                TicketStatus.CREATED.name(), dto.getSubmitterId(), "工单创建");

        log.info("[工单] 创建 id={} no={} category={} priority={} submitter={}",
                ticket.getId(), ticket.getTicketNo(), dto.getCategory(),
                priority, dto.getSubmitterId());
        return ticket.getId();
    }

    // ============================================================
    // 状态流转动作
    // ============================================================

    @Transactional
    public void assign(TicketActionDTO dto) {
        if (dto.getAssigneeId() == null) {
            throw new IllegalArgumentException("分配必须指定 assigneeId");
        }
        SysTicket ticket = transition(dto, TicketStatus.ASSIGNED,
                SysTicketLog.ACTION_ASSIGN, "分配给用户 " + dto.getAssigneeId());
        ticket.setAssigneeId(dto.getAssigneeId());
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void start(TicketActionDTO dto) {
        transition(dto, TicketStatus.IN_PROGRESS, SysTicketLog.ACTION_REPLY,
                "开始处理");
    }

    @Transactional
    public void reply(TicketActionDTO dto) {
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        SysTicket ticket = ticketMapper.selectById(dto.getTicketId());
        if (ticket == null) throw new IllegalArgumentException("工单不存在");
        // 回复不强制改状态（可在 ASSIGNED / IN_PROGRESS 内回复）
        writeLog(ticket.getId(), SysTicketLog.ACTION_REPLY,
                ticket.getStatus(), ticket.getStatus(),
                dto.getOperatorId(), dto.getContent());
    }

    @Transactional
    public void resolve(TicketActionDTO dto) {
        SysTicket ticket = transition(dto, TicketStatus.RESOLVED,
                SysTicketLog.ACTION_RESOLVE,
                dto.getContent() == null ? "已解决" : dto.getContent());
        ticket.setResolvedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void close(TicketActionDTO dto) {
        SysTicket ticket = transition(dto, TicketStatus.CLOSED,
                SysTicketLog.ACTION_CLOSE,
                dto.getContent() == null ? "已关闭" : dto.getContent());
        ticket.setClosedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
    }

    @Transactional
    public void cancel(TicketActionDTO dto) {
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("取消必须填写原因");
        }
        SysTicket ticket = transition(dto, TicketStatus.CANCELLED,
                SysTicketLog.ACTION_CANCEL, dto.getContent());
        ticket.setClosedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
    }

    /** 重开（已解决 → 处理中） */
    @Transactional
    public void reopen(TicketActionDTO dto) {
        SysTicket ticket = transition(dto, TicketStatus.IN_PROGRESS,
                SysTicketLog.ACTION_REOPEN,
                dto.getContent() == null ? "重新打开" : dto.getContent());
        ticket.setResolvedAt(null);
        ticketMapper.updateById(ticket);
    }

    // ============================================================
    // 查询
    // ============================================================

    public List<SysTicket> listMyOpen(Long userId) {
        return ticketMapper.selectList(new LambdaQueryWrapper<SysTicket>()
                .and(w -> w.eq(SysTicket::getSubmitterId, userId)
                          .or().eq(SysTicket::getAssigneeId, userId))
                .notIn(SysTicket::getStatus, "CLOSED", "CANCELLED")
                .orderByDesc(SysTicket::getCreateTime));
    }

    public List<SysTicketLog> getLogs(Long ticketId) {
        return logMapper.selectList(new LambdaQueryWrapper<SysTicketLog>()
                .eq(SysTicketLog::getTicketId, ticketId)
                .orderByAsc(SysTicketLog::getCreateTime));
    }

    // ============================================================
    // 内部辅助
    // ============================================================

    /** 通用状态流转 */
    private SysTicket transition(TicketActionDTO dto, TicketStatus to,
                                 String action, String logContent) {
        if (dto == null || dto.getTicketId() == null) {
            throw new IllegalArgumentException("工单ID不能为空");
        }
        if (dto.getOperatorId() == null) {
            throw new IllegalArgumentException("操作人不能为空");
        }

        SysTicket ticket = ticketMapper.selectById(dto.getTicketId());
        if (ticket == null) throw new IllegalArgumentException("工单不存在");

        TicketStatus from = TicketStatus.valueOf(ticket.getStatus());
        Set<TicketStatus> allowed = from.allowedNext();
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                    String.format("状态 %s 不可流转到 %s（允许: %s）", from, to, allowed));
        }

        ticket.setStatus(to.name());
        ticket.setUpdateBy(dto.getOperatorId());
        ticketMapper.updateById(ticket);

        writeLog(ticket.getId(), action, from.name(), to.name(),
                dto.getOperatorId(), logContent);
        return ticket;
    }

    private void writeLog(Long ticketId, String action, String fromStatus,
                          String toStatus, Long operatorId, String content) {
        logMapper.insert(SysTicketLog.builder()
                .ticketId(ticketId).action(action)
                .fromStatus(fromStatus).toStatus(toStatus)
                .operatorId(operatorId).content(content)
                .createTime(LocalDateTime.now())
                .build());
    }

    /** 工单编号 TKT-yyyymmdd-NNNN（4 位随机） */
    private String generateTicketNo() {
        return "TKT-" + LocalDateTime.now().format(DATE_FMT) + "-"
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
