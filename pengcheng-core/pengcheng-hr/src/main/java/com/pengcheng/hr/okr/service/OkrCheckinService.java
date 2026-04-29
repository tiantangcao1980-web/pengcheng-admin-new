package com.pengcheng.hr.okr.service;

import com.pengcheng.hr.okr.dto.CheckinDTO;
import com.pengcheng.hr.okr.entity.OkrCheckin;

import java.util.List;

/**
 * OKR Check-in 服务
 */
public interface OkrCheckinService {

    /**
     * 提交 check-in；同一 objectiveId + weekIndex 允许追加多条（每次快照）
     */
    Long submit(CheckinDTO dto);

    /**
     * 查询某目标的所有 check-in（按时间降序）
     */
    List<OkrCheckin> listByObjective(Long objectiveId);

    /**
     * 查询某用户在某周期所有目标的 check-in
     *
     * @param userId   用户 ID
     * @param periodId 周期 ID（通过 objective.periodId 关联）
     */
    List<OkrCheckin> listByUserPeriod(Long userId, Long periodId);
}
