package com.pengcheng.hr.okr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.okr.dto.CreateObjectiveDTO;
import com.pengcheng.hr.okr.entity.OkrKeyResult;
import com.pengcheng.hr.okr.entity.OkrObjective;
import com.pengcheng.hr.okr.entity.OkrPeriod;
import com.pengcheng.hr.okr.mapper.OkrKeyResultMapper;
import com.pengcheng.hr.okr.mapper.OkrObjectiveMapper;
import com.pengcheng.hr.okr.mapper.OkrPeriodMapper;
import com.pengcheng.hr.okr.service.impl.OkrObjectiveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OkrObjectiveServiceImpl 单元测试 — 6 用例
 */
@DisplayName("OkrObjectiveServiceImpl — 目标服务单元测试")
class OkrObjectiveServiceImplTest {

    private OkrObjectiveMapper objectiveMapper;
    private OkrKeyResultMapper keyResultMapper;
    private OkrPeriodMapper periodMapper;
    private OkrObjectiveServiceImpl service;

    @BeforeEach
    void setUp() {
        objectiveMapper = mock(OkrObjectiveMapper.class);
        keyResultMapper = mock(OkrKeyResultMapper.class);
        periodMapper = mock(OkrPeriodMapper.class);
        service = new OkrObjectiveServiceImpl(objectiveMapper, keyResultMapper, periodMapper);
    }

    // ---------- 用例 1：create 正常创建 ----------

    @Test
    @DisplayName("create：周期进行中 → 正常插入并返回 id")
    void create_success_whenPeriodActive() {
        OkrPeriod activePeriod = new OkrPeriod();
        activePeriod.setId(1L);
        activePeriod.setCode("2026Q2");
        activePeriod.setStatus(OkrPeriod.STATUS_ACTIVE);
        when(periodMapper.selectById(1L)).thenReturn(activePeriod);

        // insert 会把 id 设到对象上；这里 mock 让 insert 后 id=100
        doAnswer(inv -> {
            OkrObjective obj = inv.getArgument(0);
            obj.setId(100L);
            return 1;
        }).when(objectiveMapper).insert(any(OkrObjective.class));

        CreateObjectiveDTO dto = new CreateObjectiveDTO();
        dto.setPeriodId(1L);
        dto.setOwnerId(10L);
        dto.setOwnerType(OkrObjective.OWNER_USER);
        dto.setTitle("2026Q2 核心目标");

        Long id = service.create(dto);
        assertThat(id).isEqualTo(100L);
        verify(objectiveMapper).insert(any(OkrObjective.class));
    }

    // ---------- 用例 2：recalcProgress 按 KR 加权平均算 ----------

    @Test
    @DisplayName("recalcProgress：3 个 KR 进度 20/50/80，权重相等 → Objective progress = 50")
    void recalcProgress_weightedAverage() {
        OkrKeyResult kr1 = OkrKeyResult.builder().id(1L).objectiveId(5L).progress(20).weight(25).build();
        OkrKeyResult kr2 = OkrKeyResult.builder().id(2L).objectiveId(5L).progress(50).weight(25).build();
        OkrKeyResult kr3 = OkrKeyResult.builder().id(3L).objectiveId(5L).progress(80).weight(25).build();
        when(keyResultMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kr1, kr2, kr3));

        service.recalcProgress(5L);

        // 期望 progress = (20*25 + 50*25 + 80*25) / 75 = 3750/75 = 50
        verify(objectiveMapper).updateById(argThat(obj -> obj.getProgress() == 50));
    }

    // ---------- 用例 3：recalcProgress — 无 KR 时不修改进度 ----------

    @Test
    @DisplayName("recalcProgress：无 KR → 不调用 updateById")
    void recalcProgress_noKrDoesNotUpdate() {
        when(keyResultMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.recalcProgress(5L);

        verify(objectiveMapper, never()).updateById(any());
    }

    // ---------- 用例 4：对齐树查询（不同 ownerType） ----------

    @Test
    @DisplayName("listByOwnerAndPeriod：ownerType=DEPT → 查询条件含 DEPT")
    void listByOwnerAndPeriod_deptOwnerType() {
        OkrObjective deptObj = OkrObjective.builder()
                .id(20L).ownerId(3L).ownerType(OkrObjective.OWNER_DEPT).periodId(1L)
                .title("部门目标").progress(0).build();
        when(objectiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(deptObj));

        List<OkrObjective> result = service.listByOwnerAndPeriod(3L, OkrObjective.OWNER_DEPT, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwnerType()).isEqualTo(OkrObjective.OWNER_DEPT);
    }

    // ---------- 用例 5：已结束周期拒绝写入 ----------

    @Test
    @DisplayName("create：周期已结束 → 抛出 IllegalStateException")
    void create_rejectWhenPeriodClosed() {
        OkrPeriod closedPeriod = new OkrPeriod();
        closedPeriod.setId(2L);
        closedPeriod.setCode("2026Q1");
        closedPeriod.setStatus(OkrPeriod.STATUS_CLOSED);
        when(periodMapper.selectById(2L)).thenReturn(closedPeriod);

        CreateObjectiveDTO dto = new CreateObjectiveDTO();
        dto.setPeriodId(2L);
        dto.setOwnerId(10L);
        dto.setTitle("测试目标");

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已结束");

        verify(objectiveMapper, never()).insert(any());
    }

    // ---------- 用例 6：进度回退（KR 降低时重新算） ----------

    @Test
    @DisplayName("recalcProgress：KR 进度回退 100→30 → Objective 进度随之降低到 30")
    void recalcProgress_progressRollback() {
        // 只有一个 KR，进度 30
        OkrKeyResult kr = OkrKeyResult.builder().id(10L).objectiveId(7L).progress(30).weight(100).build();
        when(keyResultMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(kr));

        service.recalcProgress(7L);

        verify(objectiveMapper).updateById(argThat(obj -> obj.getProgress() == 30));
    }
}
