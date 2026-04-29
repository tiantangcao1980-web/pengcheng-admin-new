package com.pengcheng.realty.commission.service;

import com.pengcheng.realty.commission.dto.CommissionApprovalActionDTO;
import com.pengcheng.realty.commission.dto.CommissionSubmitDTO;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.entity.CommissionApproval;
import com.pengcheng.realty.commission.enums.CommissionApprovalNode;
import com.pengcheng.realty.commission.event.CommissionApprovalEvent;
import com.pengcheng.realty.commission.mapper.CommissionApprovalMapper;
import com.pengcheng.realty.commission.mapper.CommissionChangeLogMapper;
import com.pengcheng.realty.commission.mapper.CommissionDetailMapper;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.common.exception.ApprovalFlowException;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.CustomerProjectMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.project.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 佣金多级审批流单元测试
 * 覆盖：业务员提交 → 主管 → 财务 → 放款 + 各种驳回路径 + 边界
 */
@DisplayName("CommissionService approval flow")
class CommissionApprovalFlowTest {

    private CommissionMapper commissionMapper;
    private CommissionApprovalMapper commissionApprovalMapper;
    private ApplicationEventPublisher eventPublisher;
    private CommissionService service;

    private static final Long COMMISSION_ID = 1001L;
    private static final Long SUBMITTER_ID = 2001L;
    private static final Long MANAGER_ID = 3001L;
    private static final Long FINANCE_ID = 4001L;
    private static final Long PAYMENT_ID = 5001L;

    @BeforeEach
    void setUp() {
        commissionMapper = mock(CommissionMapper.class);
        commissionApprovalMapper = mock(CommissionApprovalMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new CommissionService(
                commissionMapper,
                mock(CommissionDetailMapper.class),
                mock(CommissionChangeLogMapper.class),
                commissionApprovalMapper,
                mock(CustomerDealMapper.class),
                mock(RealtyCustomerMapper.class),
                mock(CustomerProjectMapper.class),
                mock(ProjectService.class),
                mock(CommissionCalculator.class),
                eventPublisher
        );
    }

    private Commission stubCommission(String node) {
        Commission c = Commission.builder().approvalNode(node).build();
        c.setId(COMMISSION_ID);
        when(commissionMapper.selectById(COMMISSION_ID)).thenReturn(c);
        return c;
    }

    // ============================================================
    // submitForApproval
    // ============================================================

    @Test
    @DisplayName("提交：DRAFT → SUBMITTED 写入提交人/时间 + 发事件")
    void submit_fromDraft_succeeds() {
        Commission c = stubCommission(null);

        service.submitForApproval(CommissionSubmitDTO.builder()
                .commissionId(COMMISSION_ID).submitterId(SUBMITTER_ID).build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.SUBMITTED.name());
        assertThat(c.getSubmittedBy()).isEqualTo(SUBMITTER_ID);
        assertThat(c.getSubmittedTime()).isNotNull();
        verify(commissionMapper).updateById(c);

        ArgumentCaptor<CommissionApprovalEvent> evt = ArgumentCaptor.forClass(CommissionApprovalEvent.class);
        verify(eventPublisher).publishEvent(evt.capture());
        assertThat(evt.getValue().getAction()).isEqualTo(CommissionApprovalEvent.ACTION_SUBMIT);
        assertThat(evt.getValue().getToNode()).isEqualTo(CommissionApprovalNode.SUBMITTED.name());
    }

    @Test
    @DisplayName("提交：REJECTED 重提 → SUBMITTED")
    void submit_fromRejected_succeeds() {
        Commission c = stubCommission(CommissionApprovalNode.REJECTED.name());

        service.submitForApproval(CommissionSubmitDTO.builder()
                .commissionId(COMMISSION_ID).submitterId(SUBMITTER_ID).build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.SUBMITTED.name());
    }

    @Test
    @DisplayName("提交：已 SUBMITTED 状态不可重复提交")
    void submit_fromSubmitted_rejected() {
        stubCommission(CommissionApprovalNode.SUBMITTED.name());

        assertThatThrownBy(() -> service.submitForApproval(CommissionSubmitDTO.builder()
                .commissionId(COMMISSION_ID).submitterId(SUBMITTER_ID).build()))
                .isInstanceOf(ApprovalFlowException.class)
                .hasMessageContaining("不可提交");
        verify(commissionMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("提交：commissionId / submitterId 缺失 → IllegalArgumentException")
    void submit_missingArgs() {
        assertThatThrownBy(() -> service.submitForApproval(
                CommissionSubmitDTO.builder().submitterId(SUBMITTER_ID).build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submitForApproval(
                CommissionSubmitDTO.builder().commissionId(COMMISSION_ID).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============================================================
    // approveByManager
    // ============================================================

    @Test
    @DisplayName("主管通过：SUBMITTED → MANAGER_APPROVED 写审批节点记录 + 发事件")
    void manager_approve_succeeds() {
        Commission c = stubCommission(CommissionApprovalNode.SUBMITTED.name());

        service.approveByManager(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(MANAGER_ID)
                .approved(true).remark("ok").build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.MANAGER_APPROVED.name());

        ArgumentCaptor<CommissionApproval> rec = ArgumentCaptor.forClass(CommissionApproval.class);
        verify(commissionApprovalMapper).insert(rec.capture());
        assertThat(rec.getValue().getNode()).isEqualTo(CommissionApproval.NODE_MANAGER);
        assertThat(rec.getValue().getResult()).isEqualTo(CommissionApproval.RESULT_APPROVED);
        assertThat(rec.getValue().getApprovalOrder()).isEqualTo(CommissionApproval.ORDER_MANAGER);
    }

    @Test
    @DisplayName("主管驳回：SUBMITTED → REJECTED + 备注必填")
    void manager_reject_succeeds() {
        Commission c = stubCommission(CommissionApprovalNode.SUBMITTED.name());

        service.approveByManager(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(MANAGER_ID)
                .approved(false).remark("金额不对").build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.REJECTED.name());
        assertThat(c.getAuditStatus()).isEqualTo(CommissionService.AUDIT_STATUS_REJECTED);
    }

    @Test
    @DisplayName("主管驳回：未填备注 → IllegalArgumentException")
    void manager_reject_withoutRemark_throws() {
        stubCommission(CommissionApprovalNode.SUBMITTED.name());

        assertThatThrownBy(() -> service.approveByManager(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(MANAGER_ID).approved(false).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("驳回必须填写备注");
    }

    @Test
    @DisplayName("主管审批：节点不在 SUBMITTED → ApprovalFlowException")
    void manager_wrongNode_rejected() {
        stubCommission(CommissionApprovalNode.MANAGER_APPROVED.name());

        assertThatThrownBy(() -> service.approveByManager(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(MANAGER_ID).approved(true).build()))
                .isInstanceOf(ApprovalFlowException.class);
    }

    // ============================================================
    // approveByFinance
    // ============================================================

    @Test
    @DisplayName("财务通过：MANAGER_APPROVED → FINANCE_APPROVED")
    void finance_approve_succeeds() {
        Commission c = stubCommission(CommissionApprovalNode.MANAGER_APPROVED.name());

        service.approveByFinance(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(FINANCE_ID).approved(true).build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.FINANCE_APPROVED.name());
    }

    @Test
    @DisplayName("财务审批：节点不在 MANAGER_APPROVED → ApprovalFlowException")
    void finance_wrongNode_rejected() {
        stubCommission(CommissionApprovalNode.SUBMITTED.name());

        assertThatThrownBy(() -> service.approveByFinance(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(FINANCE_ID).approved(true).build()))
                .isInstanceOf(ApprovalFlowException.class);
    }

    // ============================================================
    // markPaid
    // ============================================================

    @Test
    @DisplayName("放款通过：FINANCE_APPROVED → PAID 写入 paidBy/paidTime")
    void pay_succeeds() {
        Commission c = stubCommission(CommissionApprovalNode.FINANCE_APPROVED.name());

        service.markPaid(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(PAYMENT_ID).approved(true).build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.PAID.name());
        assertThat(c.getPaidBy()).isEqualTo(PAYMENT_ID);
        assertThat(c.getPaidTime()).isNotNull();
        assertThat(c.getAuditStatus()).isEqualTo(CommissionService.AUDIT_STATUS_APPROVED);
    }

    @Test
    @DisplayName("放款驳回：FINANCE_APPROVED → REJECTED 不写 paidBy/paidTime")
    void pay_reject_doesNotSetPaidFields() {
        Commission c = stubCommission(CommissionApprovalNode.FINANCE_APPROVED.name());

        service.markPaid(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(PAYMENT_ID)
                .approved(false).remark("账户冻结").build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.REJECTED.name());
        assertThat(c.getPaidBy()).isNull();
        assertThat(c.getPaidTime()).isNull();
    }

    // ============================================================
    // 完整闭环
    // ============================================================

    @Test
    @DisplayName("完整闭环：DRAFT → SUBMITTED → MANAGER → FINANCE → PAID 共发 4 个事件")
    void fullFlow_publishes4Events() {
        Commission c = stubCommission(null);

        service.submitForApproval(CommissionSubmitDTO.builder()
                .commissionId(COMMISSION_ID).submitterId(SUBMITTER_ID).build());
        service.approveByManager(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(MANAGER_ID).approved(true).build());
        service.approveByFinance(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(FINANCE_ID).approved(true).build());
        service.markPaid(CommissionApprovalActionDTO.builder()
                .commissionId(COMMISSION_ID).approverId(PAYMENT_ID).approved(true).build());

        assertThat(c.getApprovalNode()).isEqualTo(CommissionApprovalNode.PAID.name());
        verify(eventPublisher, org.mockito.Mockito.times(4)).publishEvent(any(CommissionApprovalEvent.class));
        verify(commissionApprovalMapper, org.mockito.Mockito.times(3)).insert(any(CommissionApproval.class));
    }
}
