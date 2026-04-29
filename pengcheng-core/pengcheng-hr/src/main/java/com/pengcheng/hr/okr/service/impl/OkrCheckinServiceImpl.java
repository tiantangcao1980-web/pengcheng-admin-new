package com.pengcheng.hr.okr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.okr.dto.CheckinDTO;
import com.pengcheng.hr.okr.entity.OkrCheckin;
import com.pengcheng.hr.okr.entity.OkrObjective;
import com.pengcheng.hr.okr.mapper.OkrCheckinMapper;
import com.pengcheng.hr.okr.mapper.OkrObjectiveMapper;
import com.pengcheng.hr.okr.service.OkrCheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OKR Check-in 服务实现
 */
@Service
@RequiredArgsConstructor
public class OkrCheckinServiceImpl implements OkrCheckinService {

    private final OkrCheckinMapper checkinMapper;
    private final OkrObjectiveMapper objectiveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(CheckinDTO dto) {
        if (dto.getObjectiveId() == null) {
            throw new IllegalArgumentException("objectiveId 不能为空");
        }
        OkrObjective obj = objectiveMapper.selectById(dto.getObjectiveId());
        if (obj == null) {
            throw new IllegalArgumentException("目标不存在: " + dto.getObjectiveId());
        }

        OkrCheckin checkin = OkrCheckin.builder()
                .objectiveId(dto.getObjectiveId())
                .keyResultId(dto.getKeyResultId())
                .userId(dto.getUserId())
                .weekIndex(dto.getWeekIndex())
                .progress(dto.getProgress() != null ? dto.getProgress() : 0)
                .confidence(dto.getConfidence() != null ? dto.getConfidence() : 5)
                .summary(dto.getSummary())
                .issues(dto.getIssues())
                .nextSteps(dto.getNextSteps())
                .createTime(LocalDateTime.now())
                .build();
        checkinMapper.insert(checkin);
        return checkin.getId();
    }

    @Override
    public List<OkrCheckin> listByObjective(Long objectiveId) {
        LambdaQueryWrapper<OkrCheckin> q = new LambdaQueryWrapper<>();
        q.eq(OkrCheckin::getObjectiveId, objectiveId)
                .orderByDesc(OkrCheckin::getCreateTime);
        return checkinMapper.selectList(q);
    }

    @Override
    public List<OkrCheckin> listByUserPeriod(Long userId, Long periodId) {
        // 先查该周期下该用户的所有目标 id
        LambdaQueryWrapper<OkrObjective> objQ = new LambdaQueryWrapper<>();
        objQ.eq(OkrObjective::getPeriodId, periodId)
                .eq(OkrObjective::getOwnerId, userId)
                .eq(OkrObjective::getOwnerType, OkrObjective.OWNER_USER);
        List<OkrObjective> objs = objectiveMapper.selectList(objQ);

        if (objs.isEmpty()) {
            return List.of();
        }
        List<Long> objIds = objs.stream().map(OkrObjective::getId).toList();

        LambdaQueryWrapper<OkrCheckin> q = new LambdaQueryWrapper<>();
        q.in(OkrCheckin::getObjectiveId, objIds)
                .eq(OkrCheckin::getUserId, userId)
                .orderByDesc(OkrCheckin::getCreateTime);
        return checkinMapper.selectList(q);
    }
}
