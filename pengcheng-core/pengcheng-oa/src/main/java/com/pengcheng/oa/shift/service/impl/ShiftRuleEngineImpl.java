package com.pengcheng.oa.shift.service.impl;

import com.pengcheng.oa.shift.dto.ShiftEvaluationResult;
import com.pengcheng.oa.shift.entity.AttendanceShift;
import com.pengcheng.oa.shift.service.ShiftRuleEngine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 班次规则引擎默认实现。
 * <p>
 * 设计目标：
 * <ol>
 *   <li>固定班次：上班晚于 startTime + lateGrace 算迟到；下班早于 endTime - earlyGrace 算早退；</li>
 *   <li>跨夜班次：endTime &lt; startTime（如 22:00 -&gt; 06:00），引擎将 endTime 解释为"次日"，
 *       工时 = endDateTime - startDateTime；判定迟到/早退仍参考时间点；</li>
 *   <li>弹性班次：不强制开始/结束时间，只校验工作分钟数 &gt;= minWorkMinutes，
 *       未达到则状态 = STATUS_INSUFFICIENT；</li>
 *   <li>缺打卡：clockIn / clockOut 为 null 时返回 STATUS_MISSING。</li>
 * </ol>
 */
@Component
public class ShiftRuleEngineImpl implements ShiftRuleEngine {

    @Override
    public ShiftEvaluationResult evaluate(AttendanceShift shift, LocalDateTime clockIn, LocalDateTime clockOut) {
        if (shift == null) {
            throw new IllegalArgumentException("班次模板不能为空");
        }
        if (shift.getShiftType() == null) {
            throw new IllegalArgumentException("班次类型不能为空");
        }

        ShiftEvaluationResult.ShiftEvaluationResultBuilder builder = ShiftEvaluationResult.builder()
                .lateMinutes(0)
                .earlyMinutes(0)
                .workMinutes(0)
                .compliant(Boolean.TRUE);

        // 缺打卡判定优先
        if (clockIn == null && clockOut == null) {
            return builder
                    .clockInStatus(ShiftEvaluationResult.STATUS_MISSING)
                    .clockOutStatus(ShiftEvaluationResult.STATUS_MISSING)
                    .compliant(Boolean.FALSE)
                    .message("缺卡：未提交任何打卡记录")
                    .build();
        }

        return switch (shift.getShiftType()) {
            case AttendanceShift.TYPE_FIXED -> evaluateFixed(shift, clockIn, clockOut, builder);
            case AttendanceShift.TYPE_OVERNIGHT -> evaluateOvernight(shift, clockIn, clockOut, builder);
            case AttendanceShift.TYPE_FLEXIBLE -> evaluateFlexible(shift, clockIn, clockOut, builder);
            default -> throw new IllegalArgumentException("未知的班次类型: " + shift.getShiftType());
        };
    }

    // ========== 固定班次 ==========
    private ShiftEvaluationResult evaluateFixed(AttendanceShift shift,
                                                LocalDateTime clockIn,
                                                LocalDateTime clockOut,
                                                ShiftEvaluationResult.ShiftEvaluationResultBuilder builder) {
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            throw new IllegalArgumentException("固定班次必须配置起止时间");
        }
        int lateGrace = nullSafe(shift.getLateGraceMinutes());
        int earlyGrace = nullSafe(shift.getEarlyGraceMinutes());

        // 上班判定
        Integer clockInStatus = ShiftEvaluationResult.STATUS_MISSING;
        int lateMinutes = 0;
        if (clockIn != null) {
            LocalDateTime expectedStart = clockIn.toLocalDate().atTime(shift.getStartTime());
            long diff = Duration.between(expectedStart, clockIn).toMinutes();
            if (diff <= lateGrace) {
                clockInStatus = ShiftEvaluationResult.STATUS_NORMAL;
            } else {
                clockInStatus = ShiftEvaluationResult.STATUS_LATE;
                lateMinutes = (int) diff;
            }
        }

        // 下班判定
        Integer clockOutStatus = ShiftEvaluationResult.STATUS_MISSING;
        int earlyMinutes = 0;
        if (clockOut != null) {
            LocalDateTime expectedEnd = clockOut.toLocalDate().atTime(shift.getEndTime());
            long diff = Duration.between(clockOut, expectedEnd).toMinutes();
            if (diff <= earlyGrace) {
                clockOutStatus = ShiftEvaluationResult.STATUS_NORMAL;
            } else {
                clockOutStatus = ShiftEvaluationResult.STATUS_EARLY;
                earlyMinutes = (int) diff;
            }
        }

        int workMinutes = (clockIn != null && clockOut != null && !clockOut.isBefore(clockIn))
                ? (int) Duration.between(clockIn, clockOut).toMinutes()
                : 0;

        boolean compliant = clockInStatus.equals(ShiftEvaluationResult.STATUS_NORMAL)
                && clockOutStatus.equals(ShiftEvaluationResult.STATUS_NORMAL);

        return builder
                .clockInStatus(clockInStatus)
                .clockOutStatus(clockOutStatus)
                .lateMinutes(lateMinutes)
                .earlyMinutes(earlyMinutes)
                .workMinutes(workMinutes)
                .compliant(compliant)
                .message(buildMessage(clockInStatus, clockOutStatus, lateMinutes, earlyMinutes, workMinutes))
                .build();
    }

    // ========== 跨夜班次 ==========
    private ShiftEvaluationResult evaluateOvernight(AttendanceShift shift,
                                                    LocalDateTime clockIn,
                                                    LocalDateTime clockOut,
                                                    ShiftEvaluationResult.ShiftEvaluationResultBuilder builder) {
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            throw new IllegalArgumentException("跨夜班次必须配置起止时间");
        }
        int lateGrace = nullSafe(shift.getLateGraceMinutes());
        int earlyGrace = nullSafe(shift.getEarlyGraceMinutes());

        Integer clockInStatus = ShiftEvaluationResult.STATUS_MISSING;
        int lateMinutes = 0;
        if (clockIn != null) {
            LocalDateTime expectedStart = clockIn.toLocalDate().atTime(shift.getStartTime());
            long diff = Duration.between(expectedStart, clockIn).toMinutes();
            if (diff <= lateGrace) {
                clockInStatus = ShiftEvaluationResult.STATUS_NORMAL;
            } else {
                clockInStatus = ShiftEvaluationResult.STATUS_LATE;
                lateMinutes = (int) diff;
            }
        }

        Integer clockOutStatus = ShiftEvaluationResult.STATUS_MISSING;
        int earlyMinutes = 0;
        if (clockOut != null && clockIn != null) {
            // 跨夜：endTime 解释到 clockIn 的次日
            LocalDateTime expectedEnd = clockIn.toLocalDate().plusDays(1).atTime(shift.getEndTime());
            long diff = Duration.between(clockOut, expectedEnd).toMinutes();
            if (diff <= earlyGrace) {
                clockOutStatus = ShiftEvaluationResult.STATUS_NORMAL;
            } else {
                clockOutStatus = ShiftEvaluationResult.STATUS_EARLY;
                earlyMinutes = (int) diff;
            }
        } else if (clockOut != null) {
            // 没有 clockIn 时，按当日推算
            LocalDateTime expectedEnd = clockOut.toLocalDate().atTime(shift.getEndTime());
            long diff = Duration.between(clockOut, expectedEnd).toMinutes();
            if (diff <= earlyGrace) {
                clockOutStatus = ShiftEvaluationResult.STATUS_NORMAL;
            } else {
                clockOutStatus = ShiftEvaluationResult.STATUS_EARLY;
                earlyMinutes = (int) diff;
            }
        }

        int workMinutes = (clockIn != null && clockOut != null && clockOut.isAfter(clockIn))
                ? (int) Duration.between(clockIn, clockOut).toMinutes()
                : 0;

        boolean compliant = clockInStatus.equals(ShiftEvaluationResult.STATUS_NORMAL)
                && clockOutStatus.equals(ShiftEvaluationResult.STATUS_NORMAL);

        return builder
                .clockInStatus(clockInStatus)
                .clockOutStatus(clockOutStatus)
                .lateMinutes(lateMinutes)
                .earlyMinutes(earlyMinutes)
                .workMinutes(workMinutes)
                .compliant(compliant)
                .message(buildMessage(clockInStatus, clockOutStatus, lateMinutes, earlyMinutes, workMinutes))
                .build();
    }

    // ========== 弹性班次 ==========
    private ShiftEvaluationResult evaluateFlexible(AttendanceShift shift,
                                                   LocalDateTime clockIn,
                                                   LocalDateTime clockOut,
                                                   ShiftEvaluationResult.ShiftEvaluationResultBuilder builder) {
        int minWorkMinutes = nullSafe(shift.getMinWorkMinutes());

        if (clockIn == null || clockOut == null) {
            return builder
                    .clockInStatus(clockIn != null ? ShiftEvaluationResult.STATUS_NORMAL : ShiftEvaluationResult.STATUS_MISSING)
                    .clockOutStatus(clockOut != null ? ShiftEvaluationResult.STATUS_NORMAL : ShiftEvaluationResult.STATUS_MISSING)
                    .compliant(Boolean.FALSE)
                    .message("弹性班次缺卡，无法计算工时")
                    .build();
        }
        if (!clockOut.isAfter(clockIn)) {
            return builder
                    .clockInStatus(ShiftEvaluationResult.STATUS_NORMAL)
                    .clockOutStatus(ShiftEvaluationResult.STATUS_NORMAL)
                    .compliant(Boolean.FALSE)
                    .message("下班时间必须晚于上班时间")
                    .build();
        }

        int workMinutes = (int) Duration.between(clockIn, clockOut).toMinutes();
        boolean meetMin = workMinutes >= minWorkMinutes;
        Integer status = meetMin ? ShiftEvaluationResult.STATUS_NORMAL : ShiftEvaluationResult.STATUS_INSUFFICIENT;

        return builder
                .clockInStatus(ShiftEvaluationResult.STATUS_NORMAL)
                .clockOutStatus(status)
                .workMinutes(workMinutes)
                .compliant(meetMin)
                .message(meetMin
                        ? "弹性班次合规，工时 " + workMinutes + " 分钟"
                        : "弹性班次工时不足: " + workMinutes + " < " + minWorkMinutes + " 分钟")
                .build();
    }

    // ========== 工具方法 ==========
    private static int nullSafe(Integer v) {
        return v == null ? 0 : v;
    }

    private static String buildMessage(Integer in, Integer out, int late, int early, int work) {
        StringBuilder sb = new StringBuilder();
        if (in != null && in.equals(ShiftEvaluationResult.STATUS_LATE)) {
            sb.append("迟到 ").append(late).append(" 分钟; ");
        }
        if (out != null && out.equals(ShiftEvaluationResult.STATUS_EARLY)) {
            sb.append("早退 ").append(early).append(" 分钟; ");
        }
        if (in != null && in.equals(ShiftEvaluationResult.STATUS_MISSING)) {
            sb.append("缺上班卡; ");
        }
        if (out != null && out.equals(ShiftEvaluationResult.STATUS_MISSING)) {
            sb.append("缺下班卡; ");
        }
        if (sb.length() == 0) {
            return "正常出勤，工时 " + work + " 分钟";
        }
        return sb.toString().trim();
    }
}
