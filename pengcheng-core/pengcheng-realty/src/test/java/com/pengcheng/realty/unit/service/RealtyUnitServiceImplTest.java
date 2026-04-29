package com.pengcheng.realty.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.common.exception.InvalidStateTransitionException;
import com.pengcheng.realty.unit.dto.UnitMatrix;
import com.pengcheng.realty.unit.entity.RealtyUnit;
import com.pengcheng.realty.unit.entity.RealtyUnitStatusLog;
import com.pengcheng.realty.unit.mapper.RealtyUnitMapper;
import com.pengcheng.realty.unit.mapper.RealtyUnitStatusLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RealtyUnitService")
class RealtyUnitServiceImplTest {

    private RealtyUnitMapper unitMapper;
    private RealtyUnitStatusLogMapper logMapper;
    private RealtyUnitService service;

    @BeforeEach
    void setUp() {
        unitMapper = mock(RealtyUnitMapper.class);
        logMapper  = mock(RealtyUnitStatusLogMapper.class);
        service    = new RealtyUnitService(unitMapper, logMapper);
    }

    // ---- 1. create 自动生成 full_no ----
    @Test
    @DisplayName("create - 自动生成 full_no = building-floor-unitNo")
    void create_autoFullNo() {
        doAnswer(inv -> {
            RealtyUnit u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        }).when(unitMapper).insert(any(RealtyUnit.class));

        RealtyUnit unit = RealtyUnit.builder()
                .projectId(10L).houseTypeId(1L)
                .building("2").floor(8).unitNo("0803")
                .area(BigDecimal.valueOf(88.5)).listPrice(BigDecimal.valueOf(1_500_000))
                .build();

        Long id = service.create(unit);

        assertThat(id).isEqualTo(1L);
        assertThat(unit.getFullNo()).isEqualTo("2-8-0803");
        assertThat(unit.getStatus()).isEqualTo(RealtyUnit.STATUS_AVAILABLE);
    }

    // ---- 2. changeStatus 正向推进 AVAILABLE → RESERVED → SUBSCRIBED ----
    @Test
    @DisplayName("changeStatus - 正向推进状态机：AVAILABLE → RESERVED")
    void changeStatus_forwardTransition() {
        RealtyUnit unit = buildUnit(1L, RealtyUnit.STATUS_AVAILABLE);
        when(unitMapper.selectById(1L)).thenReturn(unit);

        service.changeStatus(1L, RealtyUnit.STATUS_RESERVED, 100L, null, null, "预留给 VIP");

        verify(unitMapper).updateById(argThat(u -> RealtyUnit.STATUS_RESERVED.equals(u.getStatus())));
        ArgumentCaptor<RealtyUnitStatusLog> logCaptor = ArgumentCaptor.forClass(RealtyUnitStatusLog.class);
        verify(logMapper).insert(logCaptor.capture());
        RealtyUnitStatusLog log = logCaptor.getValue();
        assertThat(log.getFromStatus()).isEqualTo(RealtyUnit.STATUS_AVAILABLE);
        assertThat(log.getToStatus()).isEqualTo(RealtyUnit.STATUS_RESERVED);
        assertThat(log.getOperatorId()).isEqualTo(100L);
    }

    // ---- 3. changeStatus 反向拒绝 AVAILABLE → SOLD ----
    @Test
    @DisplayName("changeStatus - 反向状态流转应抛出 InvalidStateTransitionException")
    void changeStatus_invalidTransitionThrows() {
        RealtyUnit unit = buildUnit(2L, RealtyUnit.STATUS_AVAILABLE);
        when(unitMapper.selectById(2L)).thenReturn(unit);

        assertThatThrownBy(() ->
                service.changeStatus(2L, RealtyUnit.STATUS_SOLD, 100L, null, null, null))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("AVAILABLE → SOLD");
    }

    // ---- 4. UNAVAILABLE 可从任意状态到达 ----
    @Test
    @DisplayName("changeStatus - 任意状态均可转移至 UNAVAILABLE")
    void changeStatus_anyToUnavailable() {
        List<String> fromStatuses = List.of(
                RealtyUnit.STATUS_AVAILABLE, RealtyUnit.STATUS_RESERVED,
                RealtyUnit.STATUS_SUBSCRIBED, RealtyUnit.STATUS_SIGNED,
                RealtyUnit.STATUS_SOLD);

        for (int i = 0; i < fromStatuses.size(); i++) {
            long unitId = (long) (i + 10);
            RealtyUnit unit = buildUnit(unitId, fromStatuses.get(i));
            when(unitMapper.selectById(unitId)).thenReturn(unit);

            assertThatCode(() ->
                    service.changeStatus(unitId, RealtyUnit.STATUS_UNAVAILABLE, 1L, null, null, "下架"))
                    .doesNotThrowAnyException();
        }
    }

    // ---- 5. lock 到期后 tryLock 应成功（模拟 mapper 返回 1） ----
    @Test
    @DisplayName("tryLock - 锁已过期后重新加锁成功")
    void tryLock_expiredLockSucceeds() {
        when(unitMapper.tryLock(eq(5L), eq(200L), any(LocalDateTime.class))).thenReturn(1);

        boolean result = service.tryLock(5L, 200L, 2);

        assertThat(result).isTrue();
        verify(unitMapper).tryLock(eq(5L), eq(200L), any(LocalDateTime.class));
    }

    // ---- 6. tryLock 重复加锁失败（mapper 返回 0）----
    @Test
    @DisplayName("tryLock - 已被他人锁定时返回 false")
    void tryLock_alreadyLockedReturnsFalse() {
        when(unitMapper.tryLock(eq(6L), eq(201L), any(LocalDateTime.class))).thenReturn(0);

        boolean result = service.tryLock(6L, 201L, 2);

        assertThat(result).isFalse();
    }

    // ---- 7. listMatrix 二维结构验证 ----
    @Test
    @DisplayName("listMatrix - 返回正确楼栋×楼层×房间结构")
    void listMatrix_correctStructure() {
        List<RealtyUnit> units = List.of(
                buildUnit(1L, "1", 3, "0301", RealtyUnit.STATUS_AVAILABLE),
                buildUnit(2L, "1", 3, "0302", RealtyUnit.STATUS_SOLD),
                buildUnit(3L, "1", 2, "0201", RealtyUnit.STATUS_RESERVED),
                buildUnit(4L, "2", 5, "0501", RealtyUnit.STATUS_SIGNED)
        );
        when(unitMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(units);

        List<UnitMatrix> matrix = service.listMatrix(10L);

        assertThat(matrix).hasSize(2); // 楼栋 1 和 2
        UnitMatrix bld1 = matrix.stream().filter(m -> "1".equals(m.getBuilding())).findFirst().orElseThrow();
        assertThat(bld1.getFloors()).hasSize(2); // 3 楼和 2 楼
        // 楼层从高到低：第一个应为 3 楼
        assertThat(bld1.getFloors().get(0).getFloor()).isEqualTo(3);
        assertThat(bld1.getFloors().get(0).getUnits()).hasSize(2);
    }

    // ---- 8. changeStatus SUBSCRIBED → SIGNED 正向推进（写日志含 customerId）----
    @Test
    @DisplayName("changeStatus - SUBSCRIBED → SIGNED 写日志携带 customerId")
    void changeStatus_subscribedToSigned_logsCustomer() {
        RealtyUnit unit = buildUnit(20L, RealtyUnit.STATUS_SUBSCRIBED);
        unit.setCustomerId(999L);
        when(unitMapper.selectById(20L)).thenReturn(unit);

        service.changeStatus(20L, RealtyUnit.STATUS_SIGNED, 50L, 999L, 888L, "签约成功");

        ArgumentCaptor<RealtyUnitStatusLog> cap = ArgumentCaptor.forClass(RealtyUnitStatusLog.class);
        verify(logMapper).insert(cap.capture());
        assertThat(cap.getValue().getCustomerId()).isEqualTo(999L);
        assertThat(cap.getValue().getDealId()).isEqualTo(888L);
        assertThat(cap.getValue().getToStatus()).isEqualTo(RealtyUnit.STATUS_SIGNED);
    }

    // ---- 辅助 ----
    private RealtyUnit buildUnit(Long id, String status) {
        RealtyUnit u = new RealtyUnit();
        u.setId(id);
        u.setProjectId(10L);
        u.setStatus(status);
        return u;
    }

    private RealtyUnit buildUnit(Long id, String building, int floor, String unitNo, String status) {
        RealtyUnit u = buildUnit(id, status);
        u.setBuilding(building);
        u.setFloor(floor);
        u.setUnitNo(unitNo);
        u.setFullNo(building + "-" + floor + "-" + unitNo);
        return u;
    }
}
