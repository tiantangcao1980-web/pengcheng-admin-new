package com.pengcheng.realty.customer.field.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.field.dto.FieldVisitCheckInDTO;
import com.pengcheng.realty.customer.field.dto.FieldVisitCheckOutDTO;
import com.pengcheng.realty.customer.field.entity.FieldVisit;
import com.pengcheng.realty.customer.field.event.FieldVisitEvent;
import com.pengcheng.realty.customer.field.mapper.FieldVisitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售外勤拜访服务
 *
 * 流程：签到 → 现场处理 → 签退（自动算 durationMinutes）
 * 关键事件：发布 FieldVisitEvent，由 customer 模块订阅自动写跟进时间线
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldVisitService {

    private final FieldVisitMapper fieldVisitMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 签到：创建外勤记录
     */
    @Transactional
    public Long checkIn(FieldVisitCheckInDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            throw new IllegalArgumentException("业务员ID不能为空");
        }
        // 至少关联一个：客户 或 楼盘
        if (dto.getCustomerId() == null && dto.getProjectId() == null) {
            throw new IllegalArgumentException("必须关联客户或楼盘");
        }
        if (dto.getLongitude() == null || dto.getLatitude() == null) {
            throw new IllegalArgumentException("签到必须上传 GPS 经纬度");
        }

        int visitType = dto.getVisitType() == null
                ? FieldVisit.TYPE_CUSTOMER_VISIT : dto.getVisitType();

        FieldVisit visit = FieldVisit.builder()
                .userId(dto.getUserId())
                .customerId(dto.getCustomerId())
                .projectId(dto.getProjectId())
                .visitType(visitType)
                .longitude(dto.getLongitude())
                .latitude(dto.getLatitude())
                .address(dto.getAddress())
                .photoUrls(dto.getPhotoUrls())
                .purpose(dto.getPurpose())
                .checkInTime(LocalDateTime.now())
                .build();
        fieldVisitMapper.insert(visit);

        log.info("[外勤] 签到 id={} user={} customer={} project={} type={}",
                visit.getId(), dto.getUserId(),
                dto.getCustomerId(), dto.getProjectId(), visitType);

        eventPublisher.publishEvent(new FieldVisitEvent(this,
                visit.getId(), dto.getUserId(),
                dto.getCustomerId(), dto.getProjectId(),
                FieldVisitEvent.ACTION_CHECK_IN, visitType));

        return visit.getId();
    }

    /**
     * 签退：补填结果，计算停留时长
     */
    @Transactional
    public void checkOut(FieldVisitCheckOutDTO dto) {
        if (dto == null || dto.getFieldVisitId() == null) {
            throw new IllegalArgumentException("外勤记录ID不能为空");
        }
        FieldVisit visit = fieldVisitMapper.selectById(dto.getFieldVisitId());
        if (visit == null) {
            throw new IllegalArgumentException("外勤记录不存在: " + dto.getFieldVisitId());
        }
        if (visit.getCheckOutTime() != null) {
            throw new IllegalStateException("该外勤已签退，不可重复操作");
        }
        if (dto.getUserId() != null && !dto.getUserId().equals(visit.getUserId())) {
            throw new IllegalStateException("仅本人可签退");
        }

        LocalDateTime now = LocalDateTime.now();
        visit.setCheckOutTime(now);
        visit.setResult(dto.getResult());
        if (dto.getAdditionalPhotoUrls() != null && !dto.getAdditionalPhotoUrls().isBlank()) {
            String existing = visit.getPhotoUrls() == null ? "" : visit.getPhotoUrls();
            visit.setPhotoUrls(existing.isBlank()
                    ? dto.getAdditionalPhotoUrls()
                    : existing + "," + dto.getAdditionalPhotoUrls());
        }
        long minutes = Duration.between(visit.getCheckInTime(), now).toMinutes();
        visit.setDurationMinutes((int) Math.max(minutes, 0));
        fieldVisitMapper.updateById(visit);

        log.info("[外勤] 签退 id={} 时长={}min", visit.getId(), minutes);

        eventPublisher.publishEvent(new FieldVisitEvent(this,
                visit.getId(), visit.getUserId(),
                visit.getCustomerId(), visit.getProjectId(),
                FieldVisitEvent.ACTION_CHECK_OUT, visit.getVisitType()));
    }

    /** 列出某用户某天的外勤记录（按签到时间倒序） */
    public List<FieldVisit> listByUserAndDate(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return fieldVisitMapper.selectList(new LambdaQueryWrapper<FieldVisit>()
                .eq(FieldVisit::getUserId, userId)
                .ge(FieldVisit::getCheckInTime, start)
                .lt(FieldVisit::getCheckInTime, end)
                .orderByDesc(FieldVisit::getCheckInTime));
    }

    /** 列出某客户的所有外勤拜访（用于客户档案时间线） */
    public List<FieldVisit> listByCustomer(Long customerId) {
        return fieldVisitMapper.selectList(new LambdaQueryWrapper<FieldVisit>()
                .eq(FieldVisit::getCustomerId, customerId)
                .orderByDesc(FieldVisit::getCheckInTime));
    }

    /** 当月外勤次数（KPI 用） */
    public int countByUserInMonth(Long userId, int year, int month) {
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        Long count = fieldVisitMapper.selectCount(new LambdaQueryWrapper<FieldVisit>()
                .eq(FieldVisit::getUserId, userId)
                .ge(FieldVisit::getCheckInTime, start)
                .lt(FieldVisit::getCheckInTime, end));
        return count == null ? 0 : count.intValue();
    }
}
