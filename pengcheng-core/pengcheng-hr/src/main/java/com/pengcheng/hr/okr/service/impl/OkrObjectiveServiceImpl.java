package com.pengcheng.hr.okr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.okr.dto.CreateObjectiveDTO;
import com.pengcheng.hr.okr.entity.OkrKeyResult;
import com.pengcheng.hr.okr.entity.OkrObjective;
import com.pengcheng.hr.okr.entity.OkrPeriod;
import com.pengcheng.hr.okr.mapper.OkrKeyResultMapper;
import com.pengcheng.hr.okr.mapper.OkrObjectiveMapper;
import com.pengcheng.hr.okr.mapper.OkrPeriodMapper;
import com.pengcheng.hr.okr.service.OkrObjectiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OKR 目标服务实现
 * <p>
 * recalcProgress 公式：progress = Σ(kr.progress * kr.weight) / Σ(kr.weight)
 * 若无 KR 或权重总和为 0 则保持目标原有 progress 不变。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OkrObjectiveServiceImpl implements OkrObjectiveService {

    private final OkrObjectiveMapper objectiveMapper;
    private final OkrKeyResultMapper keyResultMapper;
    private final OkrPeriodMapper periodMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateObjectiveDTO dto) {
        // 检查周期是否已结束
        OkrPeriod period = periodMapper.selectById(dto.getPeriodId());
        if (period == null) {
            throw new IllegalArgumentException("OKR 周期不存在: " + dto.getPeriodId());
        }
        if (OkrPeriod.STATUS_CLOSED == period.getStatus()) {
            throw new IllegalStateException("OKR 周期已结束，不允许新建目标: " + period.getCode());
        }

        OkrObjective obj = OkrObjective.builder()
                .periodId(dto.getPeriodId())
                .ownerId(dto.getOwnerId())
                .ownerType(dto.getOwnerType() != null ? dto.getOwnerType() : OkrObjective.OWNER_USER)
                .parentId(dto.getParentId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .progress(0)
                .weight(dto.getWeight() != null ? dto.getWeight() : 100)
                .status(OkrObjective.STATUS_ACTIVE)
                .build();
        objectiveMapper.insert(obj);
        return obj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(OkrObjective objective) {
        objectiveMapper.updateById(objective);
    }

    @Override
    public List<OkrObjective> listByOwnerAndPeriod(Long ownerId, String ownerType, Long periodId) {
        LambdaQueryWrapper<OkrObjective> q = new LambdaQueryWrapper<>();
        q.eq(OkrObjective::getOwnerId, ownerId)
                .eq(ownerType != null, OkrObjective::getOwnerType, ownerType)
                .eq(periodId != null, OkrObjective::getPeriodId, periodId)
                .orderByDesc(OkrObjective::getCreateTime);
        return objectiveMapper.selectList(q);
    }

    @Override
    public List<OkrObjective> listAlignTree(Long periodId, Long parentId) {
        LambdaQueryWrapper<OkrObjective> q = new LambdaQueryWrapper<>();
        q.eq(periodId != null, OkrObjective::getPeriodId, periodId);
        if (parentId == null) {
            q.isNull(OkrObjective::getParentId);
        } else {
            q.eq(OkrObjective::getParentId, parentId);
        }
        q.orderByDesc(OkrObjective::getCreateTime);
        return objectiveMapper.selectList(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalcProgress(Long objectiveId) {
        LambdaQueryWrapper<OkrKeyResult> q = new LambdaQueryWrapper<>();
        q.eq(OkrKeyResult::getObjectiveId, objectiveId);
        List<OkrKeyResult> krs = keyResultMapper.selectList(q);

        if (krs == null || krs.isEmpty()) {
            // 无 KR 时不修改进度
            return;
        }

        long totalWeight = 0;
        long weightedSum = 0;
        for (OkrKeyResult kr : krs) {
            int w = kr.getWeight() != null ? kr.getWeight() : 0;
            int p = kr.getProgress() != null ? kr.getProgress() : 0;
            totalWeight += w;
            weightedSum += (long) p * w;
        }

        if (totalWeight == 0) {
            return;
        }

        int newProgress = (int) (weightedSum / totalWeight);
        OkrObjective obj = new OkrObjective();
        obj.setId(objectiveId);
        obj.setProgress(newProgress);
        objectiveMapper.updateById(obj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        objectiveMapper.deleteById(id);
    }
}
