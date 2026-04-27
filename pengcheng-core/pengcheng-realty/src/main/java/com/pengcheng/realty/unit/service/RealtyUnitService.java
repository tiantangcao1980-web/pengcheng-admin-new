package com.pengcheng.realty.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.common.exception.InvalidStateTransitionException;
import com.pengcheng.realty.unit.dto.UnitMatrix;
import com.pengcheng.realty.unit.entity.RealtyUnit;
import com.pengcheng.realty.unit.entity.RealtyUnitStatusLog;
import com.pengcheng.realty.unit.mapper.RealtyUnitMapper;
import com.pengcheng.realty.unit.mapper.RealtyUnitStatusLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 房源管理服务
 *
 * <p>状态机有效转移：
 * <pre>
 * AVAILABLE → RESERVED → SUBSCRIBED → SIGNED → SOLD
 *    任意状态 → UNAVAILABLE
 *    UNAVAILABLE → AVAILABLE  (重新上架)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtyUnitService {

    // 状态机：允许的正向推进路径
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            RealtyUnit.STATUS_AVAILABLE,   Set.of(RealtyUnit.STATUS_RESERVED, RealtyUnit.STATUS_UNAVAILABLE),
            RealtyUnit.STATUS_RESERVED,    Set.of(RealtyUnit.STATUS_SUBSCRIBED, RealtyUnit.STATUS_AVAILABLE, RealtyUnit.STATUS_UNAVAILABLE),
            RealtyUnit.STATUS_SUBSCRIBED,  Set.of(RealtyUnit.STATUS_SIGNED, RealtyUnit.STATUS_AVAILABLE, RealtyUnit.STATUS_UNAVAILABLE),
            RealtyUnit.STATUS_SIGNED,      Set.of(RealtyUnit.STATUS_SOLD, RealtyUnit.STATUS_UNAVAILABLE),
            RealtyUnit.STATUS_SOLD,        Set.of(RealtyUnit.STATUS_UNAVAILABLE),
            RealtyUnit.STATUS_UNAVAILABLE, Set.of(RealtyUnit.STATUS_AVAILABLE)
    );

    private final RealtyUnitMapper realtyUnitMapper;
    private final RealtyUnitStatusLogMapper statusLogMapper;

    /**
     * 创建房源（自动生成 full_no = building-floor-unitNo）
     *
     * @return 新增记录 ID
     */
    @Transactional
    public Long create(RealtyUnit unit) {
        if (unit.getProjectId() == null) {
            throw new IllegalArgumentException("楼盘 ID 不能为空");
        }
        if (unit.getBuilding() == null || unit.getBuilding().isBlank()) {
            throw new IllegalArgumentException("楼栋号不能为空");
        }
        if (unit.getFloor() == null) {
            throw new IllegalArgumentException("楼层不能为空");
        }
        if (unit.getUnitNo() == null || unit.getUnitNo().isBlank()) {
            throw new IllegalArgumentException("房号不能为空");
        }
        // 自动生成完整编号
        unit.setFullNo(unit.getBuilding() + "-" + unit.getFloor() + "-" + unit.getUnitNo());
        if (unit.getStatus() == null) {
            unit.setStatus(RealtyUnit.STATUS_AVAILABLE);
        }
        realtyUnitMapper.insert(unit);
        return unit.getId();
    }

    /**
     * 更新房源基础信息（不含状态）
     */
    @Transactional
    public void update(RealtyUnit unit) {
        if (unit.getId() == null) {
            throw new IllegalArgumentException("房源 ID 不能为空");
        }
        // full_no 随 building/floor/unitNo 变化重新生成
        if (unit.getBuilding() != null && unit.getFloor() != null && unit.getUnitNo() != null) {
            unit.setFullNo(unit.getBuilding() + "-" + unit.getFloor() + "-" + unit.getUnitNo());
        }
        realtyUnitMapper.updateById(unit);
    }

    /**
     * 删除房源
     */
    @Transactional
    public void delete(Long id) {
        realtyUnitMapper.deleteById(id);
    }

    /**
     * 获取房源详情
     */
    public RealtyUnit getById(Long id) {
        return realtyUnitMapper.selectById(id);
    }

    /**
     * 状态变更（含状态机校验 + 写审计日志）
     *
     * @param unitId     房源 ID
     * @param toStatus   目标状态
     * @param operatorId 操作人
     * @param customerId 关联客户（可为 null）
     * @param dealId     关联成交单（可为 null）
     * @param reason     变更原因（可为 null）
     */
    @Transactional
    public void changeStatus(Long unitId, String toStatus, Long operatorId,
                             Long customerId, Long dealId, String reason) {
        RealtyUnit unit = realtyUnitMapper.selectById(unitId);
        if (unit == null) {
            throw new IllegalArgumentException("房源不存在：" + unitId);
        }
        String fromStatus = unit.getStatus();
        validateTransition(fromStatus, toStatus);

        unit.setStatus(toStatus);
        unit.setCustomerId(customerId);
        unit.setDealId(dealId);
        realtyUnitMapper.updateById(unit);

        // 写审计日志
        RealtyUnitStatusLog log = RealtyUnitStatusLog.builder()
                .unitId(unitId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .operatorId(operatorId)
                .customerId(customerId)
                .dealId(dealId)
                .reason(reason)
                .createTime(LocalDateTime.now())
                .build();
        statusLogMapper.insert(log);
    }

    /**
     * 锁定房源（阻塞式，不保证原子性 — 适合管理后台单用户场景）
     */
    @Transactional
    public void lock(Long unitId, Long userId, int hours) {
        RealtyUnit unit = realtyUnitMapper.selectById(unitId);
        if (unit == null) {
            throw new IllegalArgumentException("房源不存在：" + unitId);
        }
        unit.setLockedBy(userId);
        unit.setLockedUntil(LocalDateTime.now().plusHours(hours));
        realtyUnitMapper.updateById(unit);
    }

    /**
     * 原子加锁：仅在 AVAILABLE 且无有效锁时成功。
     *
     * @return true 加锁成功；false 已被他人锁定
     */
    @Transactional
    public boolean tryLock(Long unitId, Long userId, int hours) {
        LocalDateTime lockedUntil = LocalDateTime.now().plusHours(hours);
        int affected = realtyUnitMapper.tryLock(unitId, userId, lockedUntil);
        return affected > 0;
    }

    /**
     * 解锁房源
     */
    @Transactional
    public void unlock(Long unitId) {
        realtyUnitMapper.unlock(unitId);
    }

    /**
     * 按楼盘查询房源状态矩阵（楼栋 × 楼层 × 房间）
     * <p>前端房源状态图使用：每个楼栋独立一组，楼层从高到低，房间按 unitNo 排序。
     */
    public List<UnitMatrix> listMatrix(Long projectId) {
        LambdaQueryWrapper<RealtyUnit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RealtyUnit::getProjectId, projectId)
               .orderByAsc(RealtyUnit::getBuilding)
               .orderByDesc(RealtyUnit::getFloor)
               .orderByAsc(RealtyUnit::getUnitNo);
        List<RealtyUnit> units = realtyUnitMapper.selectList(wrapper);

        // 按楼栋分组
        Map<String, List<RealtyUnit>> byBuilding = units.stream()
                .collect(Collectors.groupingBy(RealtyUnit::getBuilding));

        return byBuilding.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String building = entry.getKey();
                    List<RealtyUnit> buildingUnits = entry.getValue();

                    // 楼层从高到低分组
                    Map<Integer, List<RealtyUnit>> byFloor = buildingUnits.stream()
                            .collect(Collectors.groupingBy(RealtyUnit::getFloor));

                    List<UnitMatrix.FloorRow> floors = byFloor.entrySet().stream()
                            .sorted(Map.Entry.<Integer, List<RealtyUnit>>comparingByKey().reversed())
                            .map(fe -> UnitMatrix.FloorRow.builder()
                                    .floor(fe.getKey())
                                    .units(fe.getValue().stream()
                                            .sorted(Comparator.comparing(RealtyUnit::getUnitNo))
                                            .collect(Collectors.toList()))
                                    .build())
                            .collect(Collectors.toList());

                    return UnitMatrix.builder()
                            .building(building)
                            .floors(floors)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 按楼盘 + 状态筛选房源列表
     */
    public List<RealtyUnit> listByStatus(Long projectId, String status) {
        LambdaQueryWrapper<RealtyUnit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RealtyUnit::getProjectId, projectId)
               .eq(status != null, RealtyUnit::getStatus, status)
               .orderByAsc(RealtyUnit::getBuilding)
               .orderByAsc(RealtyUnit::getFloor)
               .orderByAsc(RealtyUnit::getUnitNo);
        return realtyUnitMapper.selectList(wrapper);
    }

    // ---- 私有方法 ----

    /**
     * 校验状态转移是否合法
     */
    private void validateTransition(String fromStatus, String toStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.get(fromStatus);
        if (allowed == null || !allowed.contains(toStatus)) {
            throw new InvalidStateTransitionException(
                    "不允许的状态转移：" + fromStatus + " → " + toStatus);
        }
    }
}
