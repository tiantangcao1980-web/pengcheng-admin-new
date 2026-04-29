package com.pengcheng.hr.okr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.okr.entity.OkrPeriod;
import com.pengcheng.hr.okr.mapper.OkrPeriodMapper;
import com.pengcheng.hr.okr.service.OkrPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OKR 周期服务实现
 */
@Service
@RequiredArgsConstructor
public class OkrPeriodServiceImpl implements OkrPeriodService {

    private final OkrPeriodMapper periodMapper;

    @Override
    public List<OkrPeriod> listActive() {
        LambdaQueryWrapper<OkrPeriod> q = new LambdaQueryWrapper<>();
        q.eq(OkrPeriod::getStatus, OkrPeriod.STATUS_ACTIVE)
                .orderByDesc(OkrPeriod::getStartDate);
        return periodMapper.selectList(q);
    }

    @Override
    public List<OkrPeriod> listAll() {
        LambdaQueryWrapper<OkrPeriod> q = new LambdaQueryWrapper<>();
        q.orderByDesc(OkrPeriod::getStartDate);
        return periodMapper.selectList(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OkrPeriod period) {
        if (period.getStatus() == null) {
            period.setStatus(OkrPeriod.STATUS_DRAFT);
        }
        period.setCreateTime(LocalDateTime.now());
        periodMapper.insert(period);
        return period.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activatePeriod(Long id) {
        OkrPeriod period = periodMapper.selectById(id);
        if (period == null) {
            throw new IllegalArgumentException("周期不存在: " + id);
        }
        period.setStatus(OkrPeriod.STATUS_ACTIVE);
        periodMapper.updateById(period);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePeriod(Long id) {
        OkrPeriod period = periodMapper.selectById(id);
        if (period == null) {
            throw new IllegalArgumentException("周期不存在: " + id);
        }
        period.setStatus(OkrPeriod.STATUS_CLOSED);
        periodMapper.updateById(period);
    }
}
