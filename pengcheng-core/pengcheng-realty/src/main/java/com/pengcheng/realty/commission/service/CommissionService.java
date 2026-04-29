package com.pengcheng.realty.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.realty.commission.dto.*;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.entity.CommissionChangeLog;
import com.pengcheng.realty.commission.entity.CommissionDetail;
import com.pengcheng.realty.commission.entity.CommissionApproval;
import com.pengcheng.realty.commission.enums.CommissionApprovalNode;
import com.pengcheng.realty.commission.event.CommissionApprovalEvent;
import com.pengcheng.realty.commission.mapper.CommissionApprovalMapper;
import com.pengcheng.realty.commission.mapper.CommissionChangeLogMapper;
import com.pengcheng.realty.commission.mapper.CommissionDetailMapper;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import com.pengcheng.realty.common.exception.ApprovalFlowException;
import com.pengcheng.realty.common.exception.CommissionValidationException;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerProject;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerProjectMapper;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import com.pengcheng.realty.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 成交佣金管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionMapper commissionMapper;
    private final CommissionDetailMapper commissionDetailMapper;
    private final CommissionChangeLogMapper commissionChangeLogMapper;
    private final CommissionApprovalMapper commissionApprovalMapper;
    private final CustomerDealMapper customerDealMapper;
    private final RealtyCustomerMapper realtyCustomerMapper;
    private final CustomerProjectMapper customerProjectMapper;
    private final ProjectService projectService;
    private final CommissionCalculator commissionCalculator;
    private final ApplicationEventPublisher eventPublisher;

    /** 审核状态：待审核 */
    public static final int AUDIT_STATUS_PENDING = 1;
    /** 审核状态：审核通过 */
    public static final int AUDIT_STATUS_APPROVED = 2;
    /** 审核状态：审核驳回 */
    public static final int AUDIT_STATUS_REJECTED = 3;

    /**
     * 录入佣金（状态设为待审核）
     */
    @Transactional
    public Long createCommission(CommissionCreateDTO dto) {
        validateCreateDTO(dto);
        validateCommissionEquation(dto.getReceivableAmount(), dto.getPayableAmount(), dto.getPlatformFee());

        // V17：propertyType + customerOrigin 双维度，缺省走 RESIDENTIAL/DOMESTIC
        String propertyType = dto.getPropertyType() == null ? "RESIDENTIAL" : dto.getPropertyType();
        String customerOrigin = dto.getCustomerOrigin() == null ? "DOMESTIC" : dto.getCustomerOrigin();

        Commission commission = Commission.builder()
                .dealId(dto.getDealId())
                .projectId(dto.getProjectId())
                .allianceId(dto.getAllianceId())
                .receivableAmount(dto.getReceivableAmount())
                .payableAmount(dto.getPayableAmount())
                .platformFee(dto.getPlatformFee())
                .propertyType(propertyType)
                .customerOrigin(customerOrigin)
                .auditStatus(AUDIT_STATUS_PENDING)
                .build();
        commissionMapper.insert(commission);

        // 录入佣金明细（V17：含 4 角色提成字段）
        if (dto.getDetail() != null) {
            CommissionDetailDTO d = dto.getDetail();
            CommissionDetail detail = CommissionDetail.builder()
                    .commissionId(commission.getId())
                    .baseCommission(d.getBaseCommission())
                    .jumpPointCommission(d.getJumpPointCommission())
                    .cashReward(d.getCashReward())
                    .firstDealReward(d.getFirstDealReward())
                    .platformReward(d.getPlatformReward())
                    .dealerReward(d.getDealerReward())
                    .sitePersonReward(d.getSitePersonReward())
                    .channelSpecialistReward(d.getChannelSpecialistReward())
                    .channelManagerReward(d.getChannelManagerReward())
                    .build();
            commissionDetailMapper.insert(detail);
        }

        // 广播数据变更事件
        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "commission", commission.getId()));

        return commission.getId();
    }

    /**
     * 财务审核佣金（通过/驳回）
     */
    @Transactional
    public void auditCommission(CommissionAuditDTO dto) {
        if (dto.getCommissionId() == null) {
            throw new IllegalArgumentException("佣金ID不能为空");
        }
        if (dto.getApproved() == null) {
            throw new IllegalArgumentException("审核结果不能为空");
        }

        Commission commission = commissionMapper.selectById(dto.getCommissionId());
        if (commission == null) {
            throw new IllegalArgumentException("佣金记录不存在");
        }
        if (commission.getAuditStatus() != AUDIT_STATUS_PENDING) {
            throw new ApprovalFlowException("仅待审核状态的佣金记录可审核");
        }

        // 驳回时必须填写原因
        if (!dto.getApproved() && (dto.getRemark() == null || dto.getRemark().isBlank())) {
            throw new IllegalArgumentException("驳回时必须填写驳回原因");
        }

        int oldStatus = commission.getAuditStatus();
        int newStatus = dto.getApproved() ? AUDIT_STATUS_APPROVED : AUDIT_STATUS_REJECTED;

        commission.setAuditStatus(newStatus);
        commission.setAuditRemark(dto.getRemark());
        commission.setAuditorId(dto.getAuditorId());
        commission.setAuditTime(LocalDateTime.now());
        commissionMapper.updateById(commission);

        // 记录变更日志
        recordChangeLog(commission.getId(), "auditStatus",
                String.valueOf(oldStatus), String.valueOf(newStatus), dto.getAuditorId());

        // 广播数据变更事件
        eventPublisher.publishEvent(new DataChangeEvent(this, "update", "commission", commission.getId()));
    }

    /**
     * 更新佣金金额（带变更日志）
     */
    @Transactional
    public void updateCommission(Long commissionId, BigDecimal receivableAmount,
                                  BigDecimal payableAmount, BigDecimal platformFee, Long operatorId) {
        Commission commission = commissionMapper.selectById(commissionId);
        if (commission == null) {
            throw new IllegalArgumentException("佣金记录不存在");
        }

        validateCommissionEquation(receivableAmount, payableAmount, platformFee);

        // 记录各字段变更日志
        if (!Objects.equals(commission.getReceivableAmount(), receivableAmount)) {
            recordChangeLog(commissionId, "receivableAmount",
                    toString(commission.getReceivableAmount()), toString(receivableAmount), operatorId);
            commission.setReceivableAmount(receivableAmount);
        }
        if (!Objects.equals(commission.getPayableAmount(), payableAmount)) {
            recordChangeLog(commissionId, "payableAmount",
                    toString(commission.getPayableAmount()), toString(payableAmount), operatorId);
            commission.setPayableAmount(payableAmount);
        }
        if (!Objects.equals(commission.getPlatformFee(), platformFee)) {
            recordChangeLog(commissionId, "platformFee",
                    toString(commission.getPlatformFee()), toString(platformFee), operatorId);
            commission.setPlatformFee(platformFee);
        }

        commissionMapper.updateById(commission);

        // 广播数据变更事件
        eventPublisher.publishEvent(new DataChangeEvent(this, "update", "commission", commissionId));
    }

    /**
     * 分页查询佣金列表
     */
    public PageResult<CommissionVO> pageCommissions(CommissionQueryDTO query) {
        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<>();
        if (query.getProjectId() != null) {
            wrapper.eq(Commission::getProjectId, query.getProjectId());
        }
        if (query.getAllianceId() != null) {
            wrapper.eq(Commission::getAllianceId, query.getAllianceId());
        }
        if (query.getAuditStatus() != null) {
            wrapper.eq(Commission::getAuditStatus, query.getAuditStatus());
        }
        wrapper.orderByDesc(Commission::getCreateTime);

        IPage<Commission> page = commissionMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<CommissionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 检查结佣触发条件（全款到账或按揭放款）
     * 当成交记录的回款状态为全部回款(2)或贷款状态为已放款(2)时，自动发起结佣流程。
     */
    public boolean checkSettlementTrigger(Long dealId) {
        CustomerDeal deal = customerDealMapper.selectById(dealId);
        if (deal == null) {
            return false;
        }
        // 全款到账（回款状态=2）或按揭放款（贷款状态=2）
        return (deal.getPaymentStatus() != null && deal.getPaymentStatus() == 2)
                || (deal.getLoanStatus() != null && deal.getLoanStatus() == 2);
    }

    /**
     * 月末自动扫描满足结佣条件的成交记录，自动生成待审核佣金单。
     * <p>
     * 处理规则：
     * <ul>
     *   <li>触发条件：全款到账或按揭已放款</li>
     *   <li>去重：同一 dealId 已存在佣金记录则跳过</li>
     *   <li>项目归属：优先取 customer_project 最早关联记录，若多项目会告警并取首条</li>
     *   <li>结果：生成 auditStatus=待审核 的佣金单，沿用现有审批前端处理</li>
     * </ul>
     *
     * @return 本次新生成的佣金单数量
     */
    @Transactional
    public int autoCreatePendingCommissions() {
        List<CustomerDeal> candidateDeals = customerDealMapper.selectList(
                new LambdaQueryWrapper<CustomerDeal>()
                        .and(w -> w.eq(CustomerDeal::getPaymentStatus, 2)
                                .or()
                                .eq(CustomerDeal::getLoanStatus, 2))
                        .orderByAsc(CustomerDeal::getDealTime));

        int created = 0;
        for (CustomerDeal deal : candidateDeals) {
            if (deal == null || deal.getId() == null) {
                continue;
            }
            if (commissionExistsForDeal(deal.getId())) {
                continue;
            }

            Customer customer = realtyCustomerMapper.selectById(deal.getCustomerId());
            if (customer == null) {
                log.warn("[佣金自动结算] 跳过 dealId={}：客户不存在 customerId={}", deal.getId(), deal.getCustomerId());
                continue;
            }

            Long projectId = resolveProjectId(customer.getId(), deal.getId());
            if (projectId == null) {
                continue;
            }

            ProjectCommissionRule rule = projectService.getActiveCommissionRule(projectId);
            if (rule == null) {
                log.warn("[佣金自动结算] 跳过 dealId={}：项目 {} 没有生效中的佣金规则", deal.getId(), projectId);
                continue;
            }

            int dealCount = countProjectDeals(projectId, deal.getDealTime());
            CommissionCalculator.CalcResult calcResult =
                    commissionCalculator.calculate(rule, deal.getDealAmount(), dealCount);
            if (calcResult == null || !calcResult.isSuccess() || calcResult.getDetail() == null) {
                log.warn("[佣金自动结算] 跳过 dealId={}：佣金计算失败，原因={}",
                        deal.getId(), calcResult != null ? calcResult.getMessage() : "未知");
                continue;
            }

            CommissionDetailDTO detail = calcResult.getDetail();
            BigDecimal platformFee = nullToZero(detail.getPlatformReward());
            BigDecimal payableAmount = nullToZero(detail.getBaseCommission())
                    .add(nullToZero(detail.getJumpPointCommission()))
                    .add(nullToZero(detail.getCashReward()))
                    .add(nullToZero(detail.getFirstDealReward()));
            BigDecimal receivableAmount = payableAmount.add(platformFee);

            createAutoCommission(deal, customer, projectId, receivableAmount, payableAmount, platformFee, detail,
                    calcResult.getManualConfirmItems());
            created++;
        }
        log.info("[佣金自动结算] 本次自动生成 {} 条待审核佣金单", created);
        return created;
    }

    /**
     * 查询佣金变更日志
     */
    public List<CommissionChangeLog> getChangeLogs(Long commissionId) {
        LambdaQueryWrapper<CommissionChangeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommissionChangeLog::getCommissionId, commissionId)
                .orderByDesc(CommissionChangeLog::getChangeTime);
        return commissionChangeLogMapper.selectList(wrapper);
    }

    /**
     * 校验佣金等式：应收佣金 = 应结佣金 + 公司平台费
     */
    public void validateCommissionEquation(BigDecimal receivable, BigDecimal payable, BigDecimal platformFee) {
        if (receivable == null || payable == null || platformFee == null) {
            throw new IllegalArgumentException("佣金金额不能为空");
        }
        if (receivable.compareTo(payable.add(platformFee)) != 0) {
            throw new CommissionValidationException("佣金等式校验失败：应收佣金必须等于应结佣金加公司平台费");
        }
    }

    /**
     * 记录佣金变更日志
     */
    public void recordChangeLog(Long commissionId, String fieldName,
                                 String oldValue, String newValue, Long operatorId) {
        CommissionChangeLog log = CommissionChangeLog.builder()
                .commissionId(commissionId)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .operatorId(operatorId != null ? operatorId : 0L)
                .changeTime(LocalDateTime.now())
                .build();
        commissionChangeLogMapper.insert(log);
    }

    private void validateCreateDTO(CommissionCreateDTO dto) {
        if (dto.getDealId() == null) {
            throw new IllegalArgumentException("成交记录ID不能为空");
        }
        if (dto.getProjectId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        if (dto.getAllianceId() == null) {
            throw new IllegalArgumentException("联盟商ID不能为空");
        }
    }

    private CommissionVO toVO(Commission commission) {
        CommissionVO vo = CommissionVO.fromEntity(commission);
        // 查询佣金明细
        LambdaQueryWrapper<CommissionDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(CommissionDetail::getCommissionId, commission.getId())
                .last("LIMIT 1");
        CommissionDetail detail = commissionDetailMapper.selectOne(detailWrapper);
        if (detail != null) {
            vo.setDetail(CommissionDetailDTO.builder()
                    .baseCommission(detail.getBaseCommission())
                    .jumpPointCommission(detail.getJumpPointCommission())
                    .cashReward(detail.getCashReward())
                    .firstDealReward(detail.getFirstDealReward())
                    .platformReward(detail.getPlatformReward())
                    .build());
        }
        return vo;
    }

    private boolean commissionExistsForDeal(Long dealId) {
        return commissionMapper.selectCount(
                new LambdaQueryWrapper<Commission>().eq(Commission::getDealId, dealId)) > 0;
    }

    private Long resolveProjectId(Long customerId, Long dealId) {
        List<CustomerProject> relations = customerProjectMapper.selectList(
                new LambdaQueryWrapper<CustomerProject>()
                        .eq(CustomerProject::getCustomerId, customerId)
                        .orderByAsc(CustomerProject::getCreateTime)
                        .orderByAsc(CustomerProject::getId));
        if (relations == null || relations.isEmpty()) {
            log.warn("[佣金自动结算] 跳过 dealId={}：客户 {} 没有关联项目", dealId, customerId);
            return null;
        }
        if (relations.size() > 1) {
            log.warn("[佣金自动结算] dealId={} 客户 {} 关联了 {} 个项目，自动取最早关联项目 {} 生成佣金",
                    dealId, customerId, relations.size(), relations.get(0).getProjectId());
        }
        return relations.get(0).getProjectId();
    }

    private int countProjectDeals(Long projectId, LocalDateTime currentDealTime) {
        List<CustomerProject> relations = customerProjectMapper.selectList(
                new LambdaQueryWrapper<CustomerProject>().eq(CustomerProject::getProjectId, projectId));
        if (relations == null || relations.isEmpty()) {
            return 1;
        }
        List<Long> customerIds = relations.stream()
                .map(CustomerProject::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (customerIds.isEmpty()) {
            return 1;
        }
        Long count = customerDealMapper.selectCount(
                new LambdaQueryWrapper<CustomerDeal>()
                        .in(CustomerDeal::getCustomerId, customerIds)
                        .le(currentDealTime != null, CustomerDeal::getDealTime, currentDealTime));
        return count == null || count < 1 ? 1 : count.intValue();
    }

    private void createAutoCommission(CustomerDeal deal,
                                      Customer customer,
                                      Long projectId,
                                      BigDecimal receivableAmount,
                                      BigDecimal payableAmount,
                                      BigDecimal platformFee,
                                      CommissionDetailDTO detail,
                                      List<String> manualConfirmItems) {
        Commission commission = Commission.builder()
                .dealId(deal.getId())
                .projectId(projectId)
                .allianceId(customer.getAllianceId())
                .receivableAmount(receivableAmount)
                .payableAmount(payableAmount)
                .platformFee(platformFee)
                .auditStatus(AUDIT_STATUS_PENDING)
                .auditRemark(buildAutoDraftRemark(manualConfirmItems))
                .build();
        commissionMapper.insert(commission);

        commissionDetailMapper.insert(CommissionDetail.builder()
                .commissionId(commission.getId())
                .baseCommission(detail.getBaseCommission())
                .jumpPointCommission(detail.getJumpPointCommission())
                .cashReward(detail.getCashReward())
                .firstDealReward(detail.getFirstDealReward())
                .platformReward(detail.getPlatformReward())
                .build());

        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "commission", commission.getId()));
    }

    private String buildAutoDraftRemark(List<String> manualConfirmItems) {
        String prefix = "月末自动生成待审核佣金单";
        if (manualConfirmItems == null || manualConfirmItems.isEmpty()) {
            return prefix;
        }
        return prefix + "；需人工确认：" + String.join("；", manualConfirmItems);
    }

    /**
     * 按用户与日期范围汇总已审核通过应结佣金（供绩效 auto_commission 指标拉数）
     */
    public BigDecimal sumPayableByUserIdAndDateRange(Long userId, LocalDate start, LocalDate end) {
        if (userId == null || start == null || end == null) return BigDecimal.ZERO;
        LambdaQueryWrapper<Customer> cq = new LambdaQueryWrapper<>();
        cq.eq(Customer::getCreatorId, userId).select(Customer::getId);
        List<Customer> customers = realtyCustomerMapper.selectList(cq);
        if (customers == null || customers.isEmpty()) return BigDecimal.ZERO;
        List<Long> customerIds = customers.stream().map(Customer::getId).filter(Objects::nonNull).collect(Collectors.toList());
        LambdaQueryWrapper<CustomerDeal> dq = new LambdaQueryWrapper<>();
        dq.in(CustomerDeal::getCustomerId, customerIds)
          .ge(CustomerDeal::getDealTime, start.atStartOfDay())
          .lt(CustomerDeal::getDealTime, end.plusDays(1).atStartOfDay())
          .select(CustomerDeal::getId);
        List<CustomerDeal> deals = customerDealMapper.selectList(dq);
        if (deals == null || deals.isEmpty()) return BigDecimal.ZERO;
        List<Long> dealIds = deals.stream().map(CustomerDeal::getId).filter(Objects::nonNull).collect(Collectors.toList());
        LambdaQueryWrapper<Commission> w = new LambdaQueryWrapper<>();
        w.in(Commission::getDealId, dealIds).eq(Commission::getAuditStatus, AUDIT_STATUS_APPROVED);
        List<Commission> list = commissionMapper.selectList(w);
        if (list == null) return BigDecimal.ZERO;
        return list.stream()
            .map(Commission::getPayableAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String toString(BigDecimal value) {
        return value != null ? value.toPlainString() : "null";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ============================================================
    // 多级审批流（业务员 → 主管 → 财务 → 放款）
    // 旧 auditCommission(CommissionAuditDTO) 保留兼容存量调用方
    // ============================================================

    /**
     * 业务员提交审批
     * 允许的当前节点：null / DRAFT / REJECTED（重提）
     * 流转：→ SUBMITTED
     */
    @Transactional
    public void submitForApproval(CommissionSubmitDTO dto) {
        if (dto == null || dto.getCommissionId() == null) {
            throw new IllegalArgumentException("佣金ID不能为空");
        }
        if (dto.getSubmitterId() == null) {
            throw new IllegalArgumentException("提交人ID不能为空");
        }

        Commission commission = loadCommission(dto.getCommissionId());
        String currentNode = commission.getApprovalNode();

        boolean canSubmit = currentNode == null
                || CommissionApprovalNode.DRAFT.name().equals(currentNode)
                || CommissionApprovalNode.REJECTED.name().equals(currentNode);
        if (!canSubmit) {
            throw new ApprovalFlowException("当前节点不可提交：" + currentNode);
        }

        String fromNode = currentNode == null ? CommissionApprovalNode.DRAFT.name() : currentNode;
        String toNode = CommissionApprovalNode.SUBMITTED.name();

        commission.setApprovalNode(toNode);
        commission.setSubmittedBy(dto.getSubmitterId());
        commission.setSubmittedTime(LocalDateTime.now());
        commission.setAuditStatus(AUDIT_STATUS_PENDING);
        commissionMapper.updateById(commission);

        recordChangeLog(commission.getId(), "approvalNode", fromNode, toNode, dto.getSubmitterId());
        eventPublisher.publishEvent(new CommissionApprovalEvent(this, commission.getId(),
                fromNode, toNode, dto.getSubmitterId(),
                CommissionApprovalEvent.ACTION_SUBMIT, dto.getRemark()));
    }

    /**
     * 主管审批
     * 允许的当前节点：SUBMITTED
     * 通过 → MANAGER_APPROVED；驳回 → REJECTED
     */
    @Transactional
    public void approveByManager(CommissionApprovalActionDTO dto) {
        processApprovalNode(dto,
                CommissionApprovalNode.SUBMITTED,
                CommissionApprovalNode.MANAGER_APPROVED,
                CommissionApproval.NODE_MANAGER,
                CommissionApproval.ORDER_MANAGER);
    }

    /**
     * 财务审批
     * 允许的当前节点：MANAGER_APPROVED
     * 通过 → FINANCE_APPROVED；驳回 → REJECTED
     */
    @Transactional
    public void approveByFinance(CommissionApprovalActionDTO dto) {
        processApprovalNode(dto,
                CommissionApprovalNode.MANAGER_APPROVED,
                CommissionApprovalNode.FINANCE_APPROVED,
                CommissionApproval.NODE_FINANCE,
                CommissionApproval.ORDER_FINANCE);
    }

    /**
     * 放款标记
     * 允许的当前节点：FINANCE_APPROVED
     * 通过 → PAID（同时记录 paidBy / paidTime）；驳回 → REJECTED
     */
    @Transactional
    public void markPaid(CommissionApprovalActionDTO dto) {
        // 处理通用流转 + 节点记录
        processApprovalNode(dto,
                CommissionApprovalNode.FINANCE_APPROVED,
                CommissionApprovalNode.PAID,
                CommissionApproval.NODE_PAYMENT,
                CommissionApproval.ORDER_PAYMENT);

        // 通过时额外记录放款字段
        if (Boolean.TRUE.equals(dto.getApproved())) {
            Commission commission = loadCommission(dto.getCommissionId());
            commission.setPaidBy(dto.getApproverId());
            commission.setPaidTime(LocalDateTime.now());
            commission.setAuditStatus(AUDIT_STATUS_APPROVED);
            commissionMapper.updateById(commission);
        }
    }

    /**
     * 通用审批节点处理：状态机校验 + 记录审批节点 + 发事件
     */
    private void processApprovalNode(CommissionApprovalActionDTO dto,
                                     CommissionApprovalNode expectedFrom,
                                     CommissionApprovalNode approvedTo,
                                     String nodeName,
                                     int approvalOrder) {
        if (dto == null || dto.getCommissionId() == null) {
            throw new IllegalArgumentException("佣金ID不能为空");
        }
        if (dto.getApproverId() == null) {
            throw new IllegalArgumentException("审批人ID不能为空");
        }
        if (dto.getApproved() == null) {
            throw new IllegalArgumentException("审批结果不能为空");
        }
        boolean approved = dto.getApproved();
        if (!approved && (dto.getRemark() == null || dto.getRemark().isBlank())) {
            throw new IllegalArgumentException("驳回必须填写备注");
        }

        Commission commission = loadCommission(dto.getCommissionId());
        String currentNode = commission.getApprovalNode();
        if (!expectedFrom.name().equals(currentNode)) {
            throw new ApprovalFlowException(
                    String.format("当前节点 %s 不可执行 %s 审批，期望节点 %s",
                            currentNode, nodeName, expectedFrom.name()));
        }

        String toNode = approved ? approvedTo.name() : CommissionApprovalNode.REJECTED.name();
        String action = approved ? CommissionApprovalEvent.ACTION_APPROVE : CommissionApprovalEvent.ACTION_REJECT;

        // 1. 写审批节点记录
        CommissionApproval record = CommissionApproval.builder()
                .commissionId(commission.getId())
                .node(nodeName)
                .approverId(dto.getApproverId())
                .result(approved ? CommissionApproval.RESULT_APPROVED : CommissionApproval.RESULT_REJECTED)
                .remark(dto.getRemark())
                .approvalOrder(approvalOrder)
                .approvalTime(LocalDateTime.now())
                .build();
        commissionApprovalMapper.insert(record);

        // 2. 更新主表节点 + 旧字段
        commission.setApprovalNode(toNode);
        commission.setAuditRemark(dto.getRemark());
        commission.setAuditorId(dto.getApproverId());
        commission.setAuditTime(LocalDateTime.now());
        if (!approved) {
            commission.setAuditStatus(AUDIT_STATUS_REJECTED);
        }
        commissionMapper.updateById(commission);

        // 3. 变更日志 + 事件
        recordChangeLog(commission.getId(), "approvalNode", currentNode, toNode, dto.getApproverId());
        eventPublisher.publishEvent(new CommissionApprovalEvent(this, commission.getId(),
                currentNode, toNode, dto.getApproverId(), action, dto.getRemark()));
    }

    /**
     * 加载佣金记录，不存在则抛业务异常
     */
    private Commission loadCommission(Long commissionId) {
        Commission commission = commissionMapper.selectById(commissionId);
        if (commission == null) {
            throw new IllegalArgumentException("佣金记录不存在：" + commissionId);
        }
        return commission;
    }
}
