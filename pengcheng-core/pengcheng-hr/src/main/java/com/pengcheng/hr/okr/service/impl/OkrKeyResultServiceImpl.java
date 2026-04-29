package com.pengcheng.hr.okr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.okr.dto.UpdateProgressDTO;
import com.pengcheng.hr.okr.entity.OkrKeyResult;
import com.pengcheng.hr.okr.mapper.OkrKeyResultMapper;
import com.pengcheng.hr.okr.service.OkrKeyResultService;
import com.pengcheng.hr.okr.service.OkrObjectiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * OKR 关键结果服务实现
 * <p>
 * updateCurrentValue 在同一事务内更新 KR progress，随后调用
 * OkrObjectiveService.recalcProgress 重新算父 Objective 进度。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OkrKeyResultServiceImpl implements OkrKeyResultService {

    private final OkrKeyResultMapper keyResultMapper;
    private final OkrObjectiveService objectiveService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OkrKeyResult keyResult) {
        if (keyResult.getProgress() == null) {
            keyResult.setProgress(0);
        }
        if (keyResult.getWeight() == null) {
            keyResult.setWeight(25);
        }
        keyResultMapper.insert(keyResult);
        return keyResult.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(OkrKeyResult keyResult) {
        keyResultMapper.updateById(keyResult);
        if (keyResult.getObjectiveId() != null) {
            objectiveService.recalcProgress(keyResult.getObjectiveId());
        }
    }

    @Override
    public List<OkrKeyResult> listByObjective(Long objectiveId) {
        LambdaQueryWrapper<OkrKeyResult> q = new LambdaQueryWrapper<>();
        q.eq(OkrKeyResult::getObjectiveId, objectiveId)
                .orderByAsc(OkrKeyResult::getId);
        return keyResultMapper.selectList(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentValue(UpdateProgressDTO dto) {
        OkrKeyResult kr = keyResultMapper.selectById(dto.getKeyResultId());
        if (kr == null) {
            throw new IllegalArgumentException("KR 不存在: " + dto.getKeyResultId());
        }

        kr.setCurrentValue(dto.getCurrentValue());

        // 自动计算 progress
        int newProgress;
        if (dto.getProgress() != null) {
            // 手动覆盖
            newProgress = Math.max(0, Math.min(100, dto.getProgress()));
        } else {
            newProgress = calcProgress(kr);
        }
        kr.setProgress(newProgress);
        keyResultMapper.updateById(kr);

        // 联动更新父 Objective 进度（同一事务）
        objectiveService.recalcProgress(kr.getObjectiveId());
    }

    /**
     * 根据度量类型自动算 KR 进度
     */
    private int calcProgress(OkrKeyResult kr) {
        if (kr.getCurrentValue() == null || kr.getTargetValue() == null
                || kr.getTargetValue().compareTo(BigDecimal.ZERO) == 0) {
            return kr.getProgress() != null ? kr.getProgress() : 0;
        }
        String type = kr.getMeasureType();
        if (OkrKeyResult.MEASURE_BOOLEAN.equals(type)) {
            // currentValue = 1 表示完成
            return kr.getCurrentValue().compareTo(BigDecimal.ONE) >= 0 ? 100 : 0;
        }
        // NUMBER / PERCENT / MILESTONE：current / target * 100
        BigDecimal ratio = kr.getCurrentValue()
                .divide(kr.getTargetValue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        int p = ratio.intValue();
        return Math.max(0, Math.min(100, p));
    }
}
