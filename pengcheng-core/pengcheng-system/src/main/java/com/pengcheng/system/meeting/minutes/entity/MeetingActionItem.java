package com.pengcheng.system.meeting.minutes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会议行动项实体（Phase 4 J5）
 * 由 AI 纪要自动提取，关联 sys_todo
 * status: 0=待处理 1=已完成 2=取消
 */
@Data
@TableName("meeting_action_item")
public class MeetingActionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private Long minutesId;

    /** 行动项内容 */
    private String content;

    /** 负责人 userId */
    private Long ownerId;

    /** 截止日期 */
    private LocalDate dueDate;

    /** 关联的 sys_todo id，创建后回填 */
    private Long todoId;

    /** 0=待处理 1=已完成 2=取消 */
    private Integer status;

    private LocalDateTime createTime;
}
