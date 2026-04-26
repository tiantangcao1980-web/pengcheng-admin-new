package com.pengcheng.oa.shift.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班次规则引擎评估结果。
 * <p>
 * 与现有 {@code AttendanceRecord.clockInStatus / clockOutStatus} 兼容：
 * <ul>
 *   <li>{@link #STATUS_NORMAL} = 1（与 AttendanceServiceImpl 中 CLOCK_IN_NORMAL/CLOCK_OUT_NORMAL 对齐）</li>
 *   <li>{@link #STATUS_LATE} = 2（与 CLOCK_IN_LATE 对齐）</li>
 *   <li>{@link #STATUS_EARLY} = 2（与 CLOCK_OUT_EARLY 对齐）</li>
 *   <li>{@link #STATUS_INSUFFICIENT} = 3（弹性班次工时不足）</li>
 *   <li>{@link #STATUS_MISSING} = 4（未打卡）</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftEvaluationResult {

    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_LATE = 2;
    public static final int STATUS_EARLY = 2;
    public static final int STATUS_INSUFFICIENT = 3;
    public static final int STATUS_MISSING = 4;

    /** 上班打卡状态 */
    private Integer clockInStatus;

    /** 下班打卡状态 */
    private Integer clockOutStatus;

    /** 迟到分钟数 */
    private Integer lateMinutes;

    /** 早退分钟数 */
    private Integer earlyMinutes;

    /** 实际工作分钟数（覆盖跨夜班） */
    private Integer workMinutes;

    /** 是否合规 */
    private Boolean compliant;

    /** 评估说明 */
    private String message;
}
