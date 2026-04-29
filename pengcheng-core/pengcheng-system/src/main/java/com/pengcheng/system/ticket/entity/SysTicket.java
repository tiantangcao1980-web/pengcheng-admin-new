package com.pengcheng.system.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 轻量工单主表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ticket")
public class SysTicket extends BaseEntity {

    /** 工单编号 TKT-yyyymmdd-xxxx */
    private String ticketNo;

    private String title;

    private String content;

    /** 类型：IT / HR / FINANCE / OTHER */
    private String category;

    /** 优先级：1低 2中 3高 4紧急 */
    private Integer priority;

    /** 状态：CREATED / ASSIGNED / IN_PROGRESS / RESOLVED / CLOSED / CANCELLED */
    private String status;

    private Long submitterId;

    private Long assigneeId;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    /** 扩展字段 JSON（附件 URLs / 关联客户ID 等） */
    private String extra;

    public static final String CATEGORY_IT = "IT";
    public static final String CATEGORY_HR = "HR";
    public static final String CATEGORY_FINANCE = "FINANCE";
    public static final String CATEGORY_OTHER = "OTHER";

    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_URGENT = 4;
}
