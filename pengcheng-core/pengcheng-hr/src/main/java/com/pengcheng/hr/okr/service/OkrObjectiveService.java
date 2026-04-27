package com.pengcheng.hr.okr.service;

import com.pengcheng.hr.okr.dto.CreateObjectiveDTO;
import com.pengcheng.hr.okr.entity.OkrObjective;

import java.util.List;

/**
 * OKR 目标服务
 */
public interface OkrObjectiveService {

    /**
     * 创建目标；周期已结束时抛出 IllegalStateException
     */
    Long create(CreateObjectiveDTO dto);

    /**
     * 更新目标（标题/描述/权重/状态）
     */
    void update(OkrObjective objective);

    /**
     * 按 ownerId + periodId 查询目标列表
     */
    List<OkrObjective> listByOwnerAndPeriod(Long ownerId, String ownerType, Long periodId);

    /**
     * 查询以 parentId 为父节点的子目标列表（对齐树一级子节点）；
     * parentId=null 时返回顶级目标
     */
    List<OkrObjective> listAlignTree(Long periodId, Long parentId);

    /**
     * 根据该目标下所有 KR 的进度加权平均，重新计算并持久化 Objective.progress
     * <p>公式：progress = Σ(kr.progress * kr.weight) / Σ(kr.weight)；无 KR 时保持原值</p>
     */
    void recalcProgress(Long objectiveId);

    /**
     * 删除目标（逻辑）
     */
    void delete(Long id);
}
