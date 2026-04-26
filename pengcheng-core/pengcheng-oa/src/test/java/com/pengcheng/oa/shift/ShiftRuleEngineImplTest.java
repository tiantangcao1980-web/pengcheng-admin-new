package com.pengcheng.oa.shift;

import com.pengcheng.oa.shift.dto.ShiftEvaluationResult;
import com.pengcheng.oa.shift.entity.AttendanceShift;
import com.pengcheng.oa.shift.service.impl.ShiftRuleEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ShiftRuleEngineImpl - 班次规则引擎")
class ShiftRuleEngineImplTest {

    private ShiftRuleEngineImpl engine;

    @BeforeEach
    void setUp() {
        engine = new ShiftRuleEngineImpl();
    }

    // ===== 固定班次 =====

    @Test
    @DisplayName("固定班次：准时打卡为 NORMAL")
    void fixedShift_onTime() {
        AttendanceShift shift = buildFixed();

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 8, 58),
                LocalDateTime.of(2026, 4, 26, 18, 5));

        assertThat(r.getClockInStatus()).isEqualTo(ShiftEvaluationResult.STATUS_NORMAL);
        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_NORMAL);
        assertThat(r.getCompliant()).isTrue();
        assertThat(r.getWorkMinutes()).isEqualTo(9 * 60 + 7);
    }

    @Test
    @DisplayName("固定班次：迟到 15 分钟（容忍 5 分钟）")
    void fixedShift_late() {
        AttendanceShift shift = buildFixed();

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 9, 15),
                LocalDateTime.of(2026, 4, 26, 18, 0));

        assertThat(r.getClockInStatus()).isEqualTo(ShiftEvaluationResult.STATUS_LATE);
        assertThat(r.getLateMinutes()).isEqualTo(15);
        assertThat(r.getCompliant()).isFalse();
        assertThat(r.getMessage()).contains("迟到");
    }

    @Test
    @DisplayName("固定班次：早退 30 分钟")
    void fixedShift_early() {
        AttendanceShift shift = buildFixed();

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 9, 0),
                LocalDateTime.of(2026, 4, 26, 17, 30));

        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_EARLY);
        assertThat(r.getEarlyMinutes()).isEqualTo(30);
        assertThat(r.getCompliant()).isFalse();
    }

    @Test
    @DisplayName("固定班次：缺打卡返回 STATUS_MISSING")
    void fixedShift_missing() {
        AttendanceShift shift = buildFixed();

        ShiftEvaluationResult r = engine.evaluate(shift, null, null);
        assertThat(r.getClockInStatus()).isEqualTo(ShiftEvaluationResult.STATUS_MISSING);
        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_MISSING);
        assertThat(r.getCompliant()).isFalse();
    }

    @Test
    @DisplayName("固定班次：仅缺下班卡")
    void fixedShift_missing_clockOut() {
        AttendanceShift shift = buildFixed();

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 9, 0),
                null);
        assertThat(r.getClockInStatus()).isEqualTo(ShiftEvaluationResult.STATUS_NORMAL);
        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_MISSING);
    }

    // ===== 跨夜班次 =====

    @Test
    @DisplayName("跨夜班次：22:00 -> 次日 06:00 准时打卡为 NORMAL")
    void overnightShift_onTime() {
        AttendanceShift shift = buildOvernight();

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 22, 0),
                LocalDateTime.of(2026, 4, 27, 6, 5));

        assertThat(r.getClockInStatus()).isEqualTo(ShiftEvaluationResult.STATUS_NORMAL);
        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_NORMAL);
        assertThat(r.getCompliant()).isTrue();
        assertThat(r.getWorkMinutes()).isEqualTo(8 * 60 + 5);
    }

    @Test
    @DisplayName("跨夜班次：早退 1 小时（次日 05:00 打卡）")
    void overnightShift_earlyLeave() {
        AttendanceShift shift = buildOvernight();

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 22, 0),
                LocalDateTime.of(2026, 4, 27, 5, 0));

        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_EARLY);
        assertThat(r.getEarlyMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("跨夜班次：迟到 30 分钟")
    void overnightShift_late() {
        AttendanceShift shift = buildOvernight();

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 22, 30),
                LocalDateTime.of(2026, 4, 27, 6, 0));

        assertThat(r.getClockInStatus()).isEqualTo(ShiftEvaluationResult.STATUS_LATE);
        assertThat(r.getLateMinutes()).isEqualTo(30);
    }

    // ===== 弹性班次 =====

    @Test
    @DisplayName("弹性班次：满足最小工时为 NORMAL")
    void flexibleShift_meetMinimum() {
        AttendanceShift shift = buildFlexible(480);

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 10, 0),
                LocalDateTime.of(2026, 4, 26, 19, 0));

        assertThat(r.getClockInStatus()).isEqualTo(ShiftEvaluationResult.STATUS_NORMAL);
        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_NORMAL);
        assertThat(r.getCompliant()).isTrue();
        assertThat(r.getWorkMinutes()).isEqualTo(540);
    }

    @Test
    @DisplayName("弹性班次：工时不足返回 STATUS_INSUFFICIENT")
    void flexibleShift_insufficient() {
        AttendanceShift shift = buildFlexible(480);

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 10, 0),
                LocalDateTime.of(2026, 4, 26, 14, 0));

        assertThat(r.getClockOutStatus()).isEqualTo(ShiftEvaluationResult.STATUS_INSUFFICIENT);
        assertThat(r.getCompliant()).isFalse();
        assertThat(r.getWorkMinutes()).isEqualTo(240);
        assertThat(r.getMessage()).contains("工时不足");
    }

    @Test
    @DisplayName("弹性班次：缺一个打卡为非合规")
    void flexibleShift_missingHalf() {
        AttendanceShift shift = buildFlexible(480);

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 9, 0),
                null);
        assertThat(r.getCompliant()).isFalse();
    }

    @Test
    @DisplayName("弹性班次：下班 ≤ 上班视为非法")
    void flexibleShift_invalid() {
        AttendanceShift shift = buildFlexible(120);

        ShiftEvaluationResult r = engine.evaluate(shift,
                LocalDateTime.of(2026, 4, 26, 14, 0),
                LocalDateTime.of(2026, 4, 26, 10, 0));
        assertThat(r.getCompliant()).isFalse();
        assertThat(r.getMessage()).contains("晚于");
    }

    // ===== 校验异常 =====

    @Test
    @DisplayName("空班次抛异常")
    void nullShift_throws() {
        assertThatThrownBy(() -> engine.evaluate(null, LocalDateTime.now(), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("未知班次类型抛异常")
    void unknownShiftType_throws() {
        AttendanceShift bad = new AttendanceShift();
        bad.setShiftType(99);
        assertThatThrownBy(() -> engine.evaluate(bad,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("固定班次缺起止时间抛异常")
    void fixedShift_missingTimes_throws() {
        AttendanceShift bad = new AttendanceShift();
        bad.setShiftType(AttendanceShift.TYPE_FIXED);
        assertThatThrownBy(() -> engine.evaluate(bad,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== 辅助 =====

    private AttendanceShift buildFixed() {
        AttendanceShift shift = new AttendanceShift();
        shift.setShiftName("标准班");
        shift.setShiftType(AttendanceShift.TYPE_FIXED);
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(18, 0));
        shift.setLateGraceMinutes(5);
        shift.setEarlyGraceMinutes(5);
        return shift;
    }

    private AttendanceShift buildOvernight() {
        AttendanceShift shift = new AttendanceShift();
        shift.setShiftName("夜班");
        shift.setShiftType(AttendanceShift.TYPE_OVERNIGHT);
        shift.setStartTime(LocalTime.of(22, 0));
        shift.setEndTime(LocalTime.of(6, 0));
        shift.setLateGraceMinutes(0);
        shift.setEarlyGraceMinutes(0);
        return shift;
    }

    private AttendanceShift buildFlexible(int minWorkMinutes) {
        AttendanceShift shift = new AttendanceShift();
        shift.setShiftName("弹性班");
        shift.setShiftType(AttendanceShift.TYPE_FLEXIBLE);
        shift.setMinWorkMinutes(minWorkMinutes);
        return shift;
    }
}
