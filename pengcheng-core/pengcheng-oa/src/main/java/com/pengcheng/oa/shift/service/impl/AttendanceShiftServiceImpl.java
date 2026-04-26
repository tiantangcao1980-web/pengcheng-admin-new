package com.pengcheng.oa.shift.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.oa.shift.entity.AttendanceShift;
import com.pengcheng.oa.shift.mapper.AttendanceShiftMapper;
import com.pengcheng.oa.shift.service.AttendanceShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceShiftServiceImpl implements AttendanceShiftService {

    private final AttendanceShiftMapper shiftMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShift(AttendanceShift shift) {
        validate(shift);
        if (shift.getEnabled() == null) {
            shift.setEnabled(1);
        }
        shiftMapper.insert(shift);
        return shift.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShift(AttendanceShift shift) {
        if (shift.getId() == null) {
            throw new IllegalArgumentException("班次ID不能为空");
        }
        validate(shift);
        shiftMapper.updateById(shift);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShift(Long id) {
        shiftMapper.deleteById(id);
    }

    @Override
    public AttendanceShift getById(Long id) {
        return shiftMapper.selectById(id);
    }

    @Override
    public List<AttendanceShift> listEnabled() {
        LambdaQueryWrapper<AttendanceShift> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceShift::getEnabled, 1)
                .orderByAsc(AttendanceShift::getId);
        return shiftMapper.selectList(wrapper);
    }

    @Override
    public List<AttendanceShift> listAll() {
        return shiftMapper.selectList(new LambdaQueryWrapper<>());
    }

    private void validate(AttendanceShift shift) {
        if (shift.getShiftName() == null || shift.getShiftName().isBlank()) {
            throw new IllegalArgumentException("班次名称不能为空");
        }
        if (shift.getShiftType() == null) {
            throw new IllegalArgumentException("班次类型不能为空");
        }
        switch (shift.getShiftType()) {
            case AttendanceShift.TYPE_FIXED, AttendanceShift.TYPE_OVERNIGHT -> {
                if (shift.getStartTime() == null || shift.getEndTime() == null) {
                    throw new IllegalArgumentException("固定/跨夜班次必须配置起止时间");
                }
            }
            case AttendanceShift.TYPE_FLEXIBLE -> {
                if (shift.getMinWorkMinutes() == null || shift.getMinWorkMinutes() <= 0) {
                    throw new IllegalArgumentException("弹性班次必须配置最小工作分钟数");
                }
            }
            default -> throw new IllegalArgumentException("无效的班次类型: " + shift.getShiftType());
        }
    }
}
