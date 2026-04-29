package com.pengcheng.admin.controller.oa;

import com.pengcheng.common.result.Result;
import com.pengcheng.oa.shift.dto.ShiftEvaluationResult;
import com.pengcheng.oa.shift.entity.AttendanceShift;
import com.pengcheng.oa.shift.service.AttendanceShiftService;
import com.pengcheng.oa.shift.service.ShiftRuleEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OaShiftController 单元测试。
 * 使用 MockitoExtension，不启动 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OaShiftController 单元测试")
class OaShiftControllerTest {

    @Mock
    private AttendanceShiftService shiftService;

    @Mock
    private ShiftRuleEngine shiftRuleEngine;

    @InjectMocks
    private OaShiftController controller;

    // -------- 辅助工厂方法 --------

    private AttendanceShift buildShift(Long id, String name) {
        AttendanceShift s = new AttendanceShift();
        s.setId(id);
        s.setShiftName(name);
        s.setShiftType(AttendanceShift.TYPE_FIXED);
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(18, 0));
        s.setLateGraceMinutes(5);
        s.setEarlyGraceMinutes(5);
        s.setEnabled(1);
        return s;
    }

    // -------- list --------

    @Test
    @DisplayName("list — enabledOnly=null 返回全部班次")
    void list_all_returnsAll() {
        List<AttendanceShift> all = Arrays.asList(buildShift(1L, "标准班"), buildShift(2L, "夜班"));
        when(shiftService.listAll()).thenReturn(all);

        Result<List<AttendanceShift>> result = controller.list(null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
        verify(shiftService).listAll();
        verify(shiftService, never()).listEnabled();
    }

    @Test
    @DisplayName("list — enabledOnly=true 只返回启用班次")
    void list_enabledOnly_returnsEnabled() {
        List<AttendanceShift> enabled = Collections.singletonList(buildShift(1L, "标准班"));
        when(shiftService.listEnabled()).thenReturn(enabled);

        Result<List<AttendanceShift>> result = controller.list(true);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
        verify(shiftService).listEnabled();
        verify(shiftService, never()).listAll();
    }

    // -------- get by id --------

    @Test
    @DisplayName("get — 按 ID 返回指定班次")
    void get_byId_returnsShift() {
        AttendanceShift shift = buildShift(10L, "早班");
        when(shiftService.getById(10L)).thenReturn(shift);

        Result<AttendanceShift> result = controller.get(10L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getShiftName()).isEqualTo("早班");
    }

    // -------- create --------

    @Test
    @DisplayName("create — 正常创建返回新 ID")
    void create_success_returnsNewId() {
        AttendanceShift shift = buildShift(null, "弹性班");
        when(shiftService.createShift(shift)).thenReturn(99L);

        Result<Long> result = controller.create(shift);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(99L);
    }

    @Test
    @DisplayName("create — Service 抛异常时冒泡（字段缺失）")
    void create_serviceThrows_propagatesException() {
        AttendanceShift shift = new AttendanceShift(); // 缺 shiftName
        when(shiftService.createShift(shift)).thenThrow(new IllegalArgumentException("shiftName 不能为空"));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> controller.create(shift)
        );
    }

    // -------- update --------

    @Test
    @DisplayName("update — 更新成功返回 200 且 data 为 null")
    void update_success_returnsOk() {
        AttendanceShift shift = buildShift(null, "标准班 v2");
        doNothing().when(shiftService).updateShift(any(AttendanceShift.class));

        Result<Void> result = controller.update(5L, shift);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
        // 确保 id 已被注入
        assertThat(shift.getId()).isEqualTo(5L);
        verify(shiftService).updateShift(shift);
    }

    // -------- delete --------

    @Test
    @DisplayName("delete — 删除成功返回 200")
    void delete_success_returnsOk() {
        doNothing().when(shiftService).deleteShift(7L);

        Result<Void> result = controller.delete(7L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(shiftService).deleteShift(7L);
    }

    // -------- evaluate --------

    @Test
    @DisplayName("evaluate — 返回班次规则引擎的评估结果")
    void evaluate_returnsEvaluationResult() {
        AttendanceShift shift = buildShift(3L, "标准班");
        ShiftEvaluationResult evalResult = ShiftEvaluationResult.builder()
                .clockInStatus(ShiftEvaluationResult.STATUS_NORMAL)
                .clockOutStatus(ShiftEvaluationResult.STATUS_NORMAL)
                .lateMinutes(0)
                .earlyMinutes(0)
                .workMinutes(480)
                .compliant(true)
                .message("正常")
                .build();

        when(shiftService.getById(3L)).thenReturn(shift);
        LocalDateTime clockIn = LocalDateTime.now().withHour(9).withMinute(0);
        LocalDateTime clockOut = LocalDateTime.now().withHour(18).withMinute(0);
        when(shiftRuleEngine.evaluate(eq(shift), eq(clockIn), eq(clockOut))).thenReturn(evalResult);

        Result<ShiftEvaluationResult> result = controller.evaluate(3L, clockIn, clockOut);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getCompliant()).isTrue();
        assertThat(result.getData().getWorkMinutes()).isEqualTo(480);
    }
}
