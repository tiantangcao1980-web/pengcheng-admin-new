package com.pengcheng.oa.shift.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 班次模板（attendance_shift）。
 * <p>
 * 支持三种班次类型：
 * <ul>
 *   <li>{@link #TYPE_FIXED} 固定班次：strict start/end 时间</li>
 *   <li>{@link #TYPE_OVERNIGHT} 跨夜班：endTime &lt; startTime，例如 22:00 -&gt; 次日 06:00</li>
 *   <li>{@link #TYPE_FLEXIBLE} 弹性班次：必须满足最小工时（minWorkMinutes）</li>
 * </ul>
 * 迟到/早退判定结合 {@code lateGraceMinutes}（迟到容忍）与 {@code earlyGraceMinutes}（早退容忍）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_shift")
public class AttendanceShift extends BaseEntity {

    public static final int TYPE_FIXED = 1;
    public static final int TYPE_OVERNIGHT = 2;
    public static final int TYPE_FLEXIBLE = 3;

    /** 班次名称，如"标准班"、"早班"、"夜班"、"弹性班" */
    private String shiftName;

    /** 班次类型 1=固定 2=跨夜 3=弹性 */
    private Integer shiftType;

    /** 上班开始时间，弹性班次可为 null */
    private LocalTime startTime;

    /** 下班结束时间，弹性班次可为 null */
    private LocalTime endTime;

    /** 迟到容忍分钟（默认 0） */
    private Integer lateGraceMinutes;

    /** 早退容忍分钟（默认 0） */
    private Integer earlyGraceMinutes;

    /** 弹性班次最低工作分钟数（仅弹性班次有效） */
    private Integer minWorkMinutes;

    /** 备注 */
    private String remark;

    /** 是否启用：1=启用 0=停用 */
    private Integer enabled;
}
