package com.pengcheng.oa.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.oa.flow.dto.HandleApprovalDTO;
import com.pengcheng.oa.flow.dto.InstanceDetailVO;
import com.pengcheng.oa.flow.dto.StartInstanceDTO;
import com.pengcheng.oa.flow.entity.ApprovalFlowDef;
import com.pengcheng.oa.flow.entity.ApprovalFlowNode;
import com.pengcheng.oa.flow.entity.ApprovalInstance;
import com.pengcheng.oa.flow.entity.ApprovalRecord;
import com.pengcheng.oa.flow.mapper.ApprovalFlowDefMapper;
import com.pengcheng.oa.flow.mapper.ApprovalFlowNodeMapper;
import com.pengcheng.oa.flow.mapper.ApprovalInstanceMapper;
import com.pengcheng.oa.flow.mapper.ApprovalRecordMapper;
import com.pengcheng.oa.flow.service.ApprovalFlowCallback;
import com.pengcheng.oa.flow.service.ApprovalFlowEngine;
import com.pengcheng.system.eventbus.event.DomainEvent;
import com.pengcheng.system.eventbus.event.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 轻量串行审批流程引擎实现。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>单级 / 多级串行：按 nodeOrder 升序推进；</li>
 *   <li>驳回 = 立即终态（不回退到上一节点，简化 MVP）；</li>
 *   <li>超时：每个节点可配 timeoutHours + timeoutAction（pass/reject/skip）；
 *       由 {@link #sweepTimeouts()} 扫描 currentNodeDeadline ≤ now 的实例并推进；</li>
 *   <li>同节点多审批人：approverIds = "1,2,3"，任一人通过/驳回即生效（or 语义）。</li>
 * </ul>
 */
@Slf4j
@Service
public class ApprovalFlowEngineImpl implements ApprovalFlowEngine {

    private final ApprovalFlowDefMapper defMapper;
    private final ApprovalFlowNodeMapper nodeMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalRecordMapper recordMapper;
    private final List<ApprovalFlowCallback> callbacks;

    @Autowired(required = false)
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    public ApprovalFlowEngineImpl(ApprovalFlowDefMapper defMapper,
                                  ApprovalFlowNodeMapper nodeMapper,
                                  ApprovalInstanceMapper instanceMapper,
                                  ApprovalRecordMapper recordMapper,
                                  List<ApprovalFlowCallback> callbacks) {
        this.defMapper = defMapper;
        this.nodeMapper = nodeMapper;
        this.instanceMapper = instanceMapper;
        this.recordMapper = recordMapper;
        this.callbacks = callbacks == null ? Collections.emptyList() : callbacks;
    }

    // ===================== 启动 =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(StartInstanceDTO dto) {
        if (dto.getBizType() == null || dto.getBizType().isBlank()) {
            throw new IllegalArgumentException("业务类型 bizType 不能为空");
        }
        if (dto.getBizId() == null) {
            throw new IllegalArgumentException("业务实体 ID 不能为空");
        }
        if (dto.getApplicantId() == null) {
            throw new IllegalArgumentException("申请人 ID 不能为空");
        }

        ApprovalFlowDef def = resolveFlowDef(dto.getFlowDefId(), dto.getBizType());
        List<ApprovalFlowNode> nodes = listNodes(def.getId());

        ApprovalInstance instance = new ApprovalInstance();
        instance.setFlowDefId(def.getId());
        instance.setBizType(dto.getBizType());
        instance.setBizId(dto.getBizId());
        instance.setApplicantId(dto.getApplicantId());
        instance.setSummary(dto.getSummary());

        if (nodes.isEmpty()) {
            // 空流程 → 直接通过
            instance.setState(ApprovalInstance.STATE_APPROVED);
            instance.setEndTime(LocalDateTime.now());
            instanceMapper.insert(instance);
            triggerCallback(instance, true);
            return instance.getId();
        }

        ApprovalFlowNode first = nodes.get(0);
        instance.setCurrentNodeId(first.getId());
        instance.setCurrentNodeOrder(first.getNodeOrder());
        instance.setCurrentNodeDeadline(computeDeadline(first));
        instance.setState(ApprovalInstance.STATE_RUNNING);
        instanceMapper.insert(instance);
        if (domainEventPublisher != null) domainEventPublisher.publish(DomainEvent.of("approval.created", null, java.util.Map.of("id", instance.getId(), "bizType", instance.getBizType(), "bizId", String.valueOf(instance.getBizId()), "applicantId", String.valueOf(instance.getApplicantId()), "summary", instance.getSummary() == null ? "" : instance.getSummary())));
        return instance.getId();
    }

    // ===================== 处理 =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(HandleApprovalDTO dto) {
        if (dto.getInstanceId() == null) throw new IllegalArgumentException("实例ID不能为空");
        if (dto.getApproverId() == null) throw new IllegalArgumentException("审批人ID不能为空");
        if (dto.getApproved() == null) throw new IllegalArgumentException("审批结果不能为空");

        ApprovalInstance instance = mustGetInstance(dto.getInstanceId());
        if (instance.getState() != ApprovalInstance.STATE_RUNNING) {
            throw new IllegalStateException("流程已终结，不可处理");
        }
        ApprovalFlowNode currentNode = nodeMapper.selectById(instance.getCurrentNodeId());
        if (currentNode == null) {
            throw new IllegalStateException("当前节点不存在");
        }
        if (!isApproverOf(dto.getApproverId(), currentNode)) {
            throw new IllegalStateException("当前审批人无权处理：node=" + currentNode.getNodeName());
        }

        // 写记录
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instance.getId());
        record.setNodeId(currentNode.getId());
        record.setNodeOrder(currentNode.getNodeOrder());
        record.setApproverId(dto.getApproverId());
        record.setRemark(dto.getRemark());
        record.setActionTime(LocalDateTime.now());
        record.setResult(dto.getApproved() ? ApprovalRecord.RESULT_APPROVED : ApprovalRecord.RESULT_REJECTED);
        recordMapper.insert(record);

        if (Boolean.FALSE.equals(dto.getApproved())) {
            // 任一驳回 → 终态
            instance.setState(ApprovalInstance.STATE_REJECTED);
            instance.setEndTime(LocalDateTime.now());
            instanceMapper.updateById(instance);
            triggerCallback(instance, false);
            if (domainEventPublisher != null) domainEventPublisher.publish(DomainEvent.of("approval.rejected", null, java.util.Map.of("id", instance.getId(), "bizType", instance.getBizType(), "bizId", String.valueOf(instance.getBizId()), "approverId", String.valueOf(dto.getApproverId()), "remark", dto.getRemark() == null ? "" : dto.getRemark())));
            return;
        }
        // 通过 → 进入下一节点 / 终态
        advance(instance);
    }

    // ===================== 取消 =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long instanceId, Long applicantId) {
        ApprovalInstance instance = mustGetInstance(instanceId);
        if (instance.getState() != ApprovalInstance.STATE_RUNNING) {
            throw new IllegalStateException("流程已终结，无法取消");
        }
        if (!instance.getApplicantId().equals(applicantId)) {
            throw new IllegalStateException("仅申请人可取消");
        }
        instance.setState(ApprovalInstance.STATE_CANCELLED);
        instance.setEndTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        triggerCallback(instance, false);
    }

    // ===================== 超时扫描 =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sweepTimeouts() {
        LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalInstance::getState, ApprovalInstance.STATE_RUNNING)
                .isNotNull(ApprovalInstance::getCurrentNodeDeadline)
                .le(ApprovalInstance::getCurrentNodeDeadline, LocalDateTime.now());
        List<ApprovalInstance> due = instanceMapper.selectList(wrapper);
        int affected = 0;
        LocalDateTime now = LocalDateTime.now();
        for (ApprovalInstance instance : due) {
            // 二次防御：mapper 实现可能不严格过滤 deadline，这里再判一次
            if (instance.getCurrentNodeDeadline() == null
                    || instance.getCurrentNodeDeadline().isAfter(now)) {
                continue;
            }
            if (instance.getState() == null
                    || instance.getState() != ApprovalInstance.STATE_RUNNING) {
                continue;
            }
            ApprovalFlowNode node = nodeMapper.selectById(instance.getCurrentNodeId());
            if (node == null) continue;
            int action = node.getTimeoutAction() == null ? 1 : node.getTimeoutAction();
            ApprovalRecord rec = new ApprovalRecord();
            rec.setInstanceId(instance.getId());
            rec.setNodeId(node.getId());
            rec.setNodeOrder(node.getNodeOrder());
            rec.setApproverId(null);
            rec.setActionTime(LocalDateTime.now());
            rec.setRemark("节点超时自动处理");

            switch (action) {
                case 1 -> { // 自动通过
                    rec.setResult(ApprovalRecord.RESULT_TIMEOUT_PASS);
                    recordMapper.insert(rec);
                    advance(instance);
                }
                case 2 -> { // 自动驳回
                    rec.setResult(ApprovalRecord.RESULT_TIMEOUT_REJECT);
                    recordMapper.insert(rec);
                    instance.setState(ApprovalInstance.STATE_REJECTED);
                    instance.setEndTime(LocalDateTime.now());
                    instanceMapper.updateById(instance);
                    triggerCallback(instance, false);
                }
                case 3 -> { // 跳过 → 进入下一节点
                    rec.setResult(ApprovalRecord.RESULT_TIMEOUT_SKIP);
                    recordMapper.insert(rec);
                    advance(instance);
                }
                default -> {
                    // 未知动作：留作 noop，但仍记录
                    recordMapper.insert(rec);
                }
            }
            affected++;
        }
        return affected;
    }

    // ===================== 详情 =====================
    @Override
    public InstanceDetailVO detail(Long instanceId) {
        ApprovalInstance instance = mustGetInstance(instanceId);
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getInstanceId, instanceId)
                .orderByAsc(ApprovalRecord::getNodeOrder)
                .orderByAsc(ApprovalRecord::getId);
        List<ApprovalRecord> records = recordMapper.selectList(wrapper);
        return InstanceDetailVO.builder().instance(instance).records(records).build();
    }

    // ===================== 待办 =====================
    @Override
    public List<ApprovalInstance> listPending(Long approverId) {
        if (approverId == null) return Collections.emptyList();
        LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalInstance::getState, ApprovalInstance.STATE_RUNNING);
        List<ApprovalInstance> running = instanceMapper.selectList(wrapper);
        List<ApprovalInstance> result = new ArrayList<>();
        for (ApprovalInstance inst : running) {
            ApprovalFlowNode node = nodeMapper.selectById(inst.getCurrentNodeId());
            if (node != null && isApproverOf(approverId, node)) {
                result.add(inst);
            }
        }
        return result;
    }

    // ===================== 内部 =====================

    private ApprovalFlowDef resolveFlowDef(Long flowDefId, String bizType) {
        if (flowDefId != null) {
            ApprovalFlowDef def = defMapper.selectById(flowDefId);
            if (def == null) {
                throw new IllegalArgumentException("流程定义不存在: " + flowDefId);
            }
            return def;
        }
        LambdaQueryWrapper<ApprovalFlowDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowDef::getBizType, bizType)
                .eq(ApprovalFlowDef::getEnabled, 1)
                .eq(ApprovalFlowDef::getIsDefault, 1)
                .last("LIMIT 1");
        ApprovalFlowDef def = defMapper.selectOne(wrapper);
        if (def == null) {
            throw new IllegalArgumentException("未找到 bizType=" + bizType + " 的默认审批流程");
        }
        return def;
    }

    private List<ApprovalFlowNode> listNodes(Long flowDefId) {
        LambdaQueryWrapper<ApprovalFlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowNode::getFlowDefId, flowDefId)
                .orderByAsc(ApprovalFlowNode::getNodeOrder);
        return nodeMapper.selectList(wrapper);
    }

    private void advance(ApprovalInstance instance) {
        List<ApprovalFlowNode> nodes = listNodes(instance.getFlowDefId());
        Optional<ApprovalFlowNode> next = nodes.stream()
                .filter(n -> n.getNodeOrder() > instance.getCurrentNodeOrder())
                .findFirst();
        if (next.isEmpty()) {
            instance.setState(ApprovalInstance.STATE_APPROVED);
            instance.setEndTime(LocalDateTime.now());
            instance.setCurrentNodeDeadline(null);
            instanceMapper.updateById(instance);
            triggerCallback(instance, true);
            if (domainEventPublisher != null) domainEventPublisher.publish(DomainEvent.of("approval.approved", null, java.util.Map.of("id", instance.getId(), "bizType", instance.getBizType(), "bizId", String.valueOf(instance.getBizId()), "applicantId", String.valueOf(instance.getApplicantId()))));
            return;
        }
        ApprovalFlowNode nextNode = next.get();
        instance.setCurrentNodeId(nextNode.getId());
        instance.setCurrentNodeOrder(nextNode.getNodeOrder());
        instance.setCurrentNodeDeadline(computeDeadline(nextNode));
        instanceMapper.updateById(instance);
    }

    private LocalDateTime computeDeadline(ApprovalFlowNode node) {
        if (node.getTimeoutHours() == null || node.getTimeoutHours() <= 0) {
            return null;
        }
        return LocalDateTime.now().plusHours(node.getTimeoutHours());
    }

    private void triggerCallback(ApprovalInstance instance, boolean approved) {
        for (ApprovalFlowCallback cb : callbacks) {
            try {
                if (cb.supportBizType() == null) continue;
                if (cb.supportBizType().equals(instance.getBizType())) {
                    cb.onComplete(instance.getBizId(), approved);
                }
            } catch (Exception e) {
                log.error("ApprovalFlowCallback bizType={} 执行失败: {}", instance.getBizType(), e.getMessage(), e);
            }
        }
    }

    private ApprovalInstance mustGetInstance(Long id) {
        ApprovalInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + id);
        }
        return instance;
    }

    private boolean isApproverOf(Long userId, ApprovalFlowNode node) {
        if (node.getNodeType() == null || node.getNodeType() == ApprovalFlowNode.NODE_TYPE_USER) {
            if (node.getApproverIds() == null || node.getApproverIds().isBlank()) {
                return false;
            }
            return Arrays.stream(node.getApproverIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .anyMatch(s -> s.equals(String.valueOf(userId)));
        }
        // 其他类型（DEPT_HEAD/ROLE）暂不在 MVP 引擎里解析，由上层服务在 approverIds 中预先解析填充
        return false;
    }
}
