package com.pengcheng.admin.controller.oa;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.oa.correction.dto.CorrectionApplyDTO;
import com.pengcheng.oa.correction.entity.AttendanceCorrection;
import com.pengcheng.oa.correction.service.AttendanceCorrectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * OaCorrectionController 单元测试。
 * 使用 MockitoExtension，不启动 Spring 上下文。
 * {@link StpUtil#getLoginIdAsLong()} 通过 mockStatic(StpUtil.class) 隔离。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OaCorrectionController 单元测试")
class OaCorrectionControllerTest {

    @Mock
    private AttendanceCorrectionService correctionService;

    @InjectMocks
    private OaCorrectionController controller;

    // -------- 辅助工厂方法 --------

    private AttendanceCorrection buildCorrection(Long id, Long userId, Integer status) {
        AttendanceCorrection c = new AttendanceCorrection();
        c.setId(id);
        c.setUserId(userId);
        c.setCorrectionDate(LocalDate.of(2025, 4, 1));
        c.setCorrectionType(AttendanceCorrection.CORRECTION_TYPE_CLOCK_IN);
        c.setExpectedTime(LocalDateTime.of(2025, 4, 1, 9, 0));
        c.setReason("忘打卡");
        c.setStatus(status);
        return c;
    }

    private CorrectionApplyDTO buildDTO(Long userId) {
        CorrectionApplyDTO dto = new CorrectionApplyDTO();
        dto.setUserId(userId);
        dto.setCorrectionDate(LocalDate.of(2025, 4, 1));
        dto.setCorrectionType(AttendanceCorrection.CORRECTION_TYPE_CLOCK_IN);
        dto.setExpectedTime(LocalDateTime.of(2025, 4, 1, 9, 0));
        dto.setReason("忘打卡");
        return dto;
    }

    // -------- list --------

    @Test
    @DisplayName("list — 传入 userId 时直接使用，不调用 StpUtil")
    void list_withExplicitUserId_usesProvidedId() {
        List<AttendanceCorrection> records = Arrays.asList(
                buildCorrection(1L, 100L, AttendanceCorrection.STATUS_PENDING),
                buildCorrection(2L, 100L, AttendanceCorrection.STATUS_APPROVED)
        );
        when(correctionService.listByUser(100L, null)).thenReturn(records);

        Result<List<AttendanceCorrection>> result = controller.list(100L, null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
        verify(correctionService).listByUser(100L, null);
    }

    @Test
    @DisplayName("list — userId=null 时使用 StpUtil.getLoginIdAsLong()")
    void list_withoutUserId_usesStpUtil() {
        List<AttendanceCorrection> records = Collections.singletonList(
                buildCorrection(3L, 200L, AttendanceCorrection.STATUS_PENDING)
        );

        try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
            mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(200L);
            when(correctionService.listByUser(200L, null)).thenReturn(records);

            Result<List<AttendanceCorrection>> result = controller.list(null, null);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getUserId()).isEqualTo(200L);
        }
    }

    @Test
    @DisplayName("list — 按 status 过滤")
    void list_withStatus_filtersCorrectly() {
        List<AttendanceCorrection> approved = Collections.singletonList(
                buildCorrection(5L, 100L, AttendanceCorrection.STATUS_APPROVED)
        );
        when(correctionService.listByUser(100L, AttendanceCorrection.STATUS_APPROVED)).thenReturn(approved);

        Result<List<AttendanceCorrection>> result = controller.list(100L, AttendanceCorrection.STATUS_APPROVED);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().get(0).getStatus()).isEqualTo(AttendanceCorrection.STATUS_APPROVED);
    }

    // -------- get by id --------

    @Test
    @DisplayName("get — 按 ID 返回指定补卡单")
    void get_byId_returnsCorrection() {
        AttendanceCorrection c = buildCorrection(10L, 100L, AttendanceCorrection.STATUS_PENDING);
        when(correctionService.getById(10L)).thenReturn(c);

        Result<AttendanceCorrection> result = controller.get(10L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(10L);
    }

    // -------- submit --------

    @Test
    @DisplayName("submit — dto 已含 userId，直接使用，返回新 ID")
    void submit_withUserId_returnsNewId() {
        CorrectionApplyDTO dto = buildDTO(100L);
        when(correctionService.submit(dto)).thenReturn(50L);

        Result<Long> result = controller.submit(dto);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(50L);
        verify(correctionService).submit(dto);
    }

    @Test
    @DisplayName("submit — dto.userId=null 时从 StpUtil 获取当前登录人")
    void submit_withoutUserId_fillsFromStpUtil() {
        CorrectionApplyDTO dto = buildDTO(null);

        try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
            mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(300L);
            when(correctionService.submit(dto)).thenReturn(60L);

            Result<Long> result = controller.submit(dto);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(dto.getUserId()).isEqualTo(300L); // Controller 已回填
            assertThat(result.getData()).isEqualTo(60L);
        }
    }
}
