package com.pengcheng.oa.correction;

import com.pengcheng.oa.correction.dto.CorrectionApplyDTO;
import com.pengcheng.oa.correction.entity.AttendanceCorrection;
import com.pengcheng.oa.correction.mapper.AttendanceCorrectionMapper;
import com.pengcheng.oa.correction.service.impl.AttendanceCorrectionServiceImpl;
import com.pengcheng.oa.flow.dto.StartInstanceDTO;
import com.pengcheng.oa.flow.service.ApprovalFlowEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AttendanceCorrectionServiceImpl")
class AttendanceCorrectionServiceImplTest {

    private AttendanceCorrectionMapper mapper;
    private ApprovalFlowEngine flowEngine;
    private AttendanceCorrectionServiceImpl service;
    private final AtomicLong idSeq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        mapper = mock(AttendanceCorrectionMapper.class);
        flowEngine = mock(ApprovalFlowEngine.class);
        doAnswer(inv -> {
            AttendanceCorrection c = inv.getArgument(0);
            c.setId(idSeq.getAndIncrement());
            return 1;
        }).when(mapper).insert(any(AttendanceCorrection.class));
        when(flowEngine.start(any(StartInstanceDTO.class))).thenReturn(8888L);

        service = new AttendanceCorrectionServiceImpl(mapper, flowEngine);
    }

    @Test
    @DisplayName("submit：创建补卡单并启动审批流，回填 instanceId")
    void submit_ok() {
        CorrectionApplyDTO dto = new CorrectionApplyDTO();
        dto.setUserId(1L);
        dto.setCorrectionDate(LocalDate.of(2026, 4, 26));
        dto.setCorrectionType(AttendanceCorrection.CORRECTION_TYPE_CLOCK_IN);
        dto.setExpectedTime(LocalDateTime.of(2026, 4, 26, 9, 0));
        dto.setReason("忘带工牌");

        Long id = service.submit(dto);
        assertThat(id).isNotNull();

        ArgumentCaptor<StartInstanceDTO> captor = ArgumentCaptor.forClass(StartInstanceDTO.class);
        verify(flowEngine).start(captor.capture());
        assertThat(captor.getValue().getBizType()).isEqualTo("correction");
        assertThat(captor.getValue().getApplicantId()).isEqualTo(1L);

        verify(mapper).updateById(any(AttendanceCorrection.class));
    }

    @Test
    @DisplayName("submit 校验：userId 空")
    void submit_invalidUser() {
        CorrectionApplyDTO dto = new CorrectionApplyDTO();
        dto.setCorrectionDate(LocalDate.now());
        dto.setCorrectionType(1);
        dto.setExpectedTime(LocalDateTime.now());
        assertThatThrownBy(() -> service.submit(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("submit 校验：date 空")
    void submit_invalidDate() {
        CorrectionApplyDTO dto = new CorrectionApplyDTO();
        dto.setUserId(1L);
        dto.setCorrectionType(1);
        dto.setExpectedTime(LocalDateTime.now());
        assertThatThrownBy(() -> service.submit(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("submit 校验：correctionType 空")
    void submit_invalidType() {
        CorrectionApplyDTO dto = new CorrectionApplyDTO();
        dto.setUserId(1L);
        dto.setCorrectionDate(LocalDate.now());
        dto.setExpectedTime(LocalDateTime.now());
        assertThatThrownBy(() -> service.submit(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("submit 校验：expectedTime 空")
    void submit_invalidExpectedTime() {
        CorrectionApplyDTO dto = new CorrectionApplyDTO();
        dto.setUserId(1L);
        dto.setCorrectionDate(LocalDate.now());
        dto.setCorrectionType(1);
        assertThatThrownBy(() -> service.submit(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("onApprovalComplete：通过 → 状态 APPROVED")
    void onApprovalComplete_approve() {
        AttendanceCorrection c = new AttendanceCorrection();
        c.setId(1L);
        c.setStatus(AttendanceCorrection.STATUS_PENDING);
        when(mapper.selectById(1L)).thenReturn(c);

        service.onApprovalComplete(1L, true);
        assertThat(c.getStatus()).isEqualTo(AttendanceCorrection.STATUS_APPROVED);
        verify(mapper).updateById(c);
    }

    @Test
    @DisplayName("onApprovalComplete：驳回 → 状态 REJECTED")
    void onApprovalComplete_reject() {
        AttendanceCorrection c = new AttendanceCorrection();
        c.setId(2L);
        c.setStatus(AttendanceCorrection.STATUS_PENDING);
        when(mapper.selectById(2L)).thenReturn(c);

        service.onApprovalComplete(2L, false);
        assertThat(c.getStatus()).isEqualTo(AttendanceCorrection.STATUS_REJECTED);
    }

    @Test
    @DisplayName("onApprovalComplete：找不到记录则静默返回")
    void onApprovalComplete_notFound() {
        when(mapper.selectById(any())).thenReturn(null);
        service.onApprovalComplete(99L, true);
        // no exception
    }

    @Test
    @DisplayName("ApprovalFlowCallback supportBizType 与 onComplete 委托")
    void callback_delegates() {
        assertThat(service.supportBizType()).isEqualTo("correction");

        AttendanceCorrection c = new AttendanceCorrection();
        c.setId(3L);
        c.setStatus(AttendanceCorrection.STATUS_PENDING);
        when(mapper.selectById(3L)).thenReturn(c);
        service.onComplete(3L, true);
        assertThat(c.getStatus()).isEqualTo(AttendanceCorrection.STATUS_APPROVED);
    }

    @Test
    @DisplayName("getById / listByUser 转发 mapper")
    void readMethods_delegate() {
        when(mapper.selectById(any())).thenReturn(new AttendanceCorrection());
        assertThat(service.getById(1L)).isNotNull();
        service.listByUser(2L, 1);
        verify(mapper).selectList(any());
    }
}
