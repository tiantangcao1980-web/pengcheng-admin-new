package com.pengcheng.crm.lead.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.exception.BizErrorCode;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.lead.dto.LeadAssignDTO;
import com.pengcheng.crm.lead.dto.LeadConvertDTO;
import com.pengcheng.crm.lead.dto.LeadCreateDTO;
import com.pengcheng.crm.lead.entity.CrmLead;
import com.pengcheng.crm.lead.entity.CrmLeadAssignment;
import com.pengcheng.crm.lead.mapper.CrmLeadAssignmentMapper;
import com.pengcheng.crm.lead.mapper.CrmLeadMapper;
import com.pengcheng.system.eventbus.event.DomainEvent;
import com.pengcheng.system.eventbus.event.DomainEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 线索管理 Service
 */
@Service
public class LeadService {

    @Autowired
    private CrmLeadMapper leadMapper;

    @Autowired
    private CrmLeadAssignmentMapper assignmentMapper;

    @Autowired(required = false)
    private DomainEventPublisher domainEventPublisher;

    public CrmLead create(LeadCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "参数不能为空");
        }
        CrmLead lead = CrmLead.builder()
                .leadNo(generateLeadNo())
                .name(dto.getName())
                .phone(dto.getPhone())
                .phoneMasked(maskPhone(dto.getPhone()))
                .email(dto.getEmail())
                .wechat(dto.getWechat())
                .company(dto.getCompany())
                .source(dto.getSource() == null ? "manual" : dto.getSource())
                .sourceDetail(dto.getSourceDetail())
                .intentionLevel(dto.getIntentionLevel() == null ? 2 : dto.getIntentionLevel())
                .status(dto.getOwnerId() == null ? 1 : 2)
                .ownerId(dto.getOwnerId())
                .deptId(dto.getDeptId())
                .assignTime(dto.getOwnerId() == null ? null : LocalDateTime.now())
                .remark(dto.getRemark())
                .build();
        leadMapper.insert(lead);
        if (domainEventPublisher != null) domainEventPublisher.publish(DomainEvent.of("lead.created", lead.getTenantId(), java.util.Map.of("id", lead.getId(), "leadNo", lead.getLeadNo(), "name", lead.getName(), "phone", lead.getPhoneMasked(), "source", lead.getSource(), "ownerId", String.valueOf(lead.getOwnerId()), "createdBy", String.valueOf(lead.getCreateBy()))));
        return lead;
    }

    public IPage<CrmLead> page(long page, long size, Long ownerId, Integer status, String keyword) {
        Page<CrmLead> p = new Page<>(page, size);
        return leadMapper.selectPage(p,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrmLead>()
                        .eq(ownerId != null, CrmLead::getOwnerId, ownerId)
                        .eq(status != null, CrmLead::getStatus, status)
                        .like(keyword != null && !keyword.isBlank(), CrmLead::getName, keyword)
                        .orderByDesc(CrmLead::getCreateTime));
    }

    public CrmLead getById(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "线索不存在");
        }
        return lead;
    }

    @Transactional(rollbackFor = Exception.class)
    public int assign(LeadAssignDTO dto, Long currentUserId, Map<Long, Integer> currentLoad) {
        if (dto == null || dto.getLeadIds() == null || dto.getLeadIds().isEmpty()) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "leadIds 不能为空");
        }
        int affected = 0;
        for (Long leadId : dto.getLeadIds()) {
            CrmLead lead = leadMapper.selectById(leadId);
            if (lead == null) {
                continue;
            }
            Long target = LeadAssignmentRule.pick(
                    dto.getRuleType(),
                    dto.getTargetUserId(),
                    dto.getCandidateUserIds(),
                    currentLoad == null ? new HashMap<>() : currentLoad,
                    lead.getSource(),
                    null);
            if (target == null) {
                continue;
            }

            CrmLeadAssignment log = CrmLeadAssignment.builder()
                    .leadId(leadId)
                    .fromUserId(lead.getOwnerId())
                    .toUserId(target)
                    .assignedBy(currentUserId)
                    .ruleType(dto.getRuleType() == null ? "manual" : dto.getRuleType())
                    .note(dto.getNote())
                    .createTime(LocalDateTime.now())
                    .build();
            assignmentMapper.insert(log);

            lead.setOwnerId(target);
            lead.setStatus(2);
            lead.setAssignTime(LocalDateTime.now());
            leadMapper.updateById(lead);
            if (domainEventPublisher != null) domainEventPublisher.publish(DomainEvent.of("lead.assigned", lead.getTenantId(), java.util.Map.of("id", lead.getId(), "leadNo", lead.getLeadNo(), "toUserId", String.valueOf(target), "assignedBy", String.valueOf(currentUserId))));
            affected++;
        }
        return affected;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmLead convertToCustomer(LeadConvertDTO dto) {
        if (dto == null || dto.getLeadId() == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "leadId 不能为空");
        }
        CrmLead lead = getById(dto.getLeadId());
        if (Integer.valueOf(4).equals(lead.getStatus())) {
            throw new BusinessException(BizErrorCode.BUSINESS_ERROR, "线索已转客户");
        }
        // 注意：此处不直接 new Customer，避免跨模块紧耦合。
        // 由调用方（如 CRM Controller / 上层 Facade）负责创建 customer 并把 customerId 回传。
        Long customerId = dto.getCustomerId();
        if (customerId == null) {
            // 留给上层创建客户后回写；此处仅记录"已发起转化"的中间态
            throw new BusinessException(BizErrorCode.BUSINESS_ERROR,
                    "转客户必须由上层 Facade 提供 customerId（避免对 customer 业务模块的硬依赖）");
        }
        lead.setCustomerId(customerId);
        lead.setStatus(4);
        lead.setConvertTime(LocalDateTime.now());
        if (dto.getRemark() != null) {
            lead.setRemark(dto.getRemark());
        }
        leadMapper.updateById(lead);
        if (domainEventPublisher != null) domainEventPublisher.publish(DomainEvent.of("lead.converted", lead.getTenantId(), java.util.Map.of("id", lead.getId(), "leadNo", lead.getLeadNo(), "customerId", String.valueOf(lead.getCustomerId()), "convertTime", lead.getConvertTime().toString())));
        return lead;
    }

    public List<CrmLeadAssignment> assignmentLog(Long leadId) {
        return assignmentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrmLeadAssignment>()
                        .eq(CrmLeadAssignment::getLeadId, leadId)
                        .orderByDesc(CrmLeadAssignment::getCreateTime));
    }

    static String generateLeadNo() {
        return "L" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
