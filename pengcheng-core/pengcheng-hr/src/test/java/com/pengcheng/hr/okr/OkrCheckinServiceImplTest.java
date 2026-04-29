package com.pengcheng.hr.okr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.okr.dto.CheckinDTO;
import com.pengcheng.hr.okr.entity.OkrCheckin;
import com.pengcheng.hr.okr.entity.OkrObjective;
import com.pengcheng.hr.okr.mapper.OkrCheckinMapper;
import com.pengcheng.hr.okr.mapper.OkrObjectiveMapper;
import com.pengcheng.hr.okr.service.impl.OkrCheckinServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OkrCheckinServiceImpl 单元测试 — 4 用例
 */
@DisplayName("OkrCheckinServiceImpl — Check-in 服务单元测试")
class OkrCheckinServiceImplTest {

    private OkrCheckinMapper checkinMapper;
    private OkrObjectiveMapper objectiveMapper;
    private OkrCheckinServiceImpl service;

    @BeforeEach
    void setUp() {
        checkinMapper = mock(OkrCheckinMapper.class);
        objectiveMapper = mock(OkrObjectiveMapper.class);
        service = new OkrCheckinServiceImpl(checkinMapper, objectiveMapper);
    }

    // ---------- 用例 1：submit 正常提交 ----------

    @Test
    @DisplayName("submit：有效 DTO → 写库并返回 id")
    void submit_success() {
        OkrObjective obj = new OkrObjective();
        obj.setId(1L);
        obj.setTitle("目标A");
        when(objectiveMapper.selectById(1L)).thenReturn(obj);

        doAnswer(inv -> {
            OkrCheckin c = inv.getArgument(0);
            c.setId(99L);
            return 1;
        }).when(checkinMapper).insert(any(OkrCheckin.class));

        CheckinDTO dto = new CheckinDTO();
        dto.setObjectiveId(1L);
        dto.setUserId(10L);
        dto.setWeekIndex(18);
        dto.setProgress(60);
        dto.setConfidence(7);
        dto.setSummary("本周完成核心功能");

        Long id = service.submit(dto);
        assertThat(id).isEqualTo(99L);
        verify(checkinMapper).insert(argThat(c -> c.getProgress() == 60 && c.getConfidence() == 7));
    }

    // ---------- 用例 2：submit 目标不存在 → 抛异常 ----------

    @Test
    @DisplayName("submit：目标不存在 → 抛出 IllegalArgumentException")
    void submit_objectiveNotFound() {
        when(objectiveMapper.selectById(999L)).thenReturn(null);

        CheckinDTO dto = new CheckinDTO();
        dto.setObjectiveId(999L);
        dto.setUserId(10L);
        dto.setWeekIndex(1);
        dto.setProgress(0);

        assertThatThrownBy(() -> service.submit(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
        verify(checkinMapper, never()).insert(any());
    }

    // ---------- 用例 3：listByObjective 按目标查询 ----------

    @Test
    @DisplayName("listByObjective：返回目标下所有 check-in 按时间降序")
    void listByObjective_returnsList() {
        OkrCheckin c1 = OkrCheckin.builder().id(1L).objectiveId(5L).weekIndex(10).progress(40).build();
        OkrCheckin c2 = OkrCheckin.builder().id(2L).objectiveId(5L).weekIndex(11).progress(55).build();
        when(checkinMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c2, c1));

        List<OkrCheckin> result = service.listByObjective(5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getWeekIndex()).isEqualTo(11); // 较新的在前
    }

    // ---------- 用例 4：listByUserPeriod 无目标 → 返回空列表 ----------

    @Test
    @DisplayName("listByUserPeriod：用户在该周期无目标 → 返回空列表，不报错")
    void listByUserPeriod_noObjectives_returnsEmpty() {
        when(objectiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<OkrCheckin> result = service.listByUserPeriod(10L, 3L);

        assertThat(result).isEmpty();
        verify(checkinMapper, never()).selectList(any());
    }
}
