package com.pengcheng.system.ticket.enums;

import lombok.Getter;

import java.util.Set;

/**
 * 工单状态机
 *
 * 流转：
 *   CREATED ──assign──▶ ASSIGNED ──start──▶ IN_PROGRESS ──resolve──▶ RESOLVED ──close──▶ CLOSED
 *      │                  │                    │                       │
 *      └──────cancel──────┴──────cancel────────┴───────cancel──────────┘
 *
 *   RESOLVED ──reopen──▶ IN_PROGRESS（重开）
 */
@Getter
public enum TicketStatus {
    CREATED("已创建"),
    ASSIGNED("已分配"),
    IN_PROGRESS("处理中"),
    RESOLVED("已解决"),
    CLOSED("已关闭"),
    CANCELLED("已取消");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    /** 是否终态（不可再流转） */
    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    /** 该状态允许的下一步状态集合 */
    public Set<TicketStatus> allowedNext() {
        return switch (this) {
            case CREATED -> Set.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> Set.of(IN_PROGRESS, ASSIGNED, CANCELLED);  // ASSIGNED 可重新分配
            case IN_PROGRESS -> Set.of(RESOLVED, CANCELLED);
            case RESOLVED -> Set.of(CLOSED, IN_PROGRESS);  // 重开
            case CLOSED, CANCELLED -> Set.of();
        };
    }
}
