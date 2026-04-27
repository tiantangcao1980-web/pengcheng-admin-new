package com.pengcheng.system.meeting.minutes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 纪要实体（Phase 4 J5）
 * status: PENDING / TRANSCRIBING / SUMMARIZING / READY / FAILED
 */
@Data
@TableName("meeting_minutes_ai")
public class MeetingMinutesAi {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对应预订 id（唯一） */
    private Long bookingId;

    /** 录音 OSS 路径 */
    private String audioUrl;

    /** ASR 转写全文 */
    private String transcript;

    /** LLM 摘要 */
    private String summary;

    /** PENDING / TRANSCRIBING / SUMMARIZING / READY / FAILED */
    private String status;

    /** 失败原因 */
    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
