package com.pengcheng.system.meeting.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议签到实体（Phase 4 J5）
 * sign_type: QRCODE / MANUAL / NFC
 */
@Data
@TableName("meeting_attendance")
public class MeetingAttendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private Long userId;

    private LocalDateTime signTime;

    /** QRCODE / MANUAL / NFC */
    private String signType;
}
