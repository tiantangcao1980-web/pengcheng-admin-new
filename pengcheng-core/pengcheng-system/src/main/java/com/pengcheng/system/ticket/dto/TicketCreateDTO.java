package com.pengcheng.system.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateDTO {
    private String title;
    private String content;
    /** IT / HR / FINANCE / OTHER */
    private String category;
    /** 1低 2中 3高 4紧急；不填默认 2 */
    private Integer priority;
    /** 提单人；Controller 层从 Sa-Token 注入 */
    private Long submitterId;
}
