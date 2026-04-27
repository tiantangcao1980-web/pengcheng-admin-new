package com.pengcheng.system.meeting.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议预订实体（Phase 4 J5）
 * status: 0=预订 1=进行中 2=已结束 3=取消
 */
@Data
@TableName("meeting_booking")
public class MeetingBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    /** 组织者 userId */
    private Long organizerId;

    /** 会议室 id，可为 null（纯线上会议） */
    private Long roomId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 0=预订 1=进行中 2=已结束 3=取消 */
    private Integer status;

    /** 参会者 JSON 数组 [userId, ...] */
    private String attendees;

    /** 腾讯会议/Zoom 链接 */
    private String onlineUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
