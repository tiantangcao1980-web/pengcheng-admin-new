package com.pengcheng.system.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单操作日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_ticket_log")
public class SysTicketLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;

    /** 动作：CREATE / ASSIGN / REPLY / RESOLVE / CLOSE / CANCEL / REOPEN */
    private String action;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    /** 动作内容/回复正文 */
    private String content;

    private LocalDateTime createTime;

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_ASSIGN = "ASSIGN";
    public static final String ACTION_REPLY = "REPLY";
    public static final String ACTION_RESOLVE = "RESOLVE";
    public static final String ACTION_CLOSE = "CLOSE";
    public static final String ACTION_CANCEL = "CANCEL";
    public static final String ACTION_REOPEN = "REOPEN";
}
