package com.pengcheng.hr.okr.service;

import com.pengcheng.hr.okr.dto.UpdateProgressDTO;
import com.pengcheng.hr.okr.entity.OkrKeyResult;

import java.util.List;

/**
 * OKR 关键结果服务
 */
public interface OkrKeyResultService {

    /**
     * 新建 KR
     */
    Long create(OkrKeyResult keyResult);

    /**
     * 更新 KR 基本信息（标题/度量类型/目标值/权重等）
     */
    void update(OkrKeyResult keyResult);

    /**
     * 查询目标下所有 KR
     */
    List<OkrKeyResult> listByObjective(Long objectiveId);

    /**
     * 更新 KR 当前值，自动计算 progress，并触发父 Objective recalcProgress
     */
    void updateCurrentValue(UpdateProgressDTO dto);
}
