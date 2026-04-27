package com.pengcheng.system.meeting.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室实体（Phase 4 J5）
 */
@Data
@TableName("meeting_room")
public class MeetingRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室名称 */
    private String name;

    /** 位置 */
    private String location;

    /** 容纳人数 */
    private Integer capacity;

    /** 设施，逗号分隔：投影/白板/视频/电话 */
    private String facilities;

    /** 是否启用：1启用 0停用 */
    private Integer enabled;

    private LocalDateTime createTime;
}
