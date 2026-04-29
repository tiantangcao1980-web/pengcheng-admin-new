package com.pengcheng.system.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工单动作 DTO（assign / reply / resolve / close / cancel / reopen 共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketActionDTO {

    private Long ticketId;

    /** 操作人 */
    private Long operatorId;

    /** 分配/转交时使用 */
    private Long assigneeId;

    /** 回复内容 / 解决方案 / 取消原因 等 */
    private String content;
}
