package com.pengcheng.oa.shift;

import com.pengcheng.oa.shift.entity.AttendanceShift;
import com.pengcheng.oa.shift.mapper.AttendanceShiftMapper;
import com.pengcheng.oa.shift.service.impl.AttendanceShiftServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AttendanceShiftServiceImpl")
class AttendanceShiftServiceImplTest {

    private AttendanceShiftMapper mapper;
    private AttendanceShiftServiceImpl service;
    private final AtomicLong idSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        mapper = mock(AttendanceShiftMapper.class);
        doAnswer(inv -> {
            AttendanceShift s = inv.getArgument(0);
            s.setId(idSeq.getAndIncrement());
            return 1;
        }).when(mapper).insert(any(AttendanceShift.class));
        when(mapper.selectList(any())).thenReturn(new ArrayList<>());
        service = new AttendanceShiftServiceImpl(mapper);
    }

    @Test
    @DisplayName("create：固定班次合法")
    void create_fixedOk() {
        AttendanceShift s = new AttendanceShift();
        s.setShiftName("标准班");
        s.setShiftType(AttendanceShift.TYPE_FIXED);
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(18, 0));

        Long id = service.createShift(s);
        assertThat(id).isNotNull();
        assertThat(s.getEnabled()).isEqualTo(1);
    }

    @Test
    @DisplayName("create：跨夜班次需要起止时间")
    void create_overnight_missingTime() {
        AttendanceShift s = new AttendanceShift();
        s.setShiftName("夜班");
        s.setShiftType(AttendanceShift.TYPE_OVERNIGHT);
        assertThatThrownBy(() -> service.createShift(s)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create：弹性班次需要 minWorkMinutes > 0")
    void create_flexible_missingMinWork() {
        AttendanceShift s = new AttendanceShift();
        s.setShiftName("弹性班");
        s.setShiftType(AttendanceShift.TYPE_FLEXIBLE);
        s.setMinWorkMinutes(0);
        assertThatThrownBy(() -> service.createShift(s)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create：name 空抛异常")
    void create_emptyName() {
        AttendanceShift s = new AttendanceShift();
        s.setShiftType(1);
        assertThatThrownBy(() -> service.createShift(s)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create：type 空抛异常")
    void create_emptyType() {
        AttendanceShift s = new AttendanceShift();
        s.setShiftName("x");
        assertThatThrownBy(() -> service.createShift(s)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create：未知 type 抛异常")
    void create_unknownType() {
        AttendanceShift s = new AttendanceShift();
        s.setShiftName("x");
        s.setShiftType(99);
        assertThatThrownBy(() -> service.createShift(s)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update：缺 ID 抛异常")
    void update_missingId() {
        AttendanceShift s = new AttendanceShift();
        s.setShiftName("x");
        s.setShiftType(1);
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(18, 0));
        assertThatThrownBy(() -> service.updateShift(s)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update：成功调用 updateById")
    void update_ok() {
        AttendanceShift s = new AttendanceShift();
        s.setId(1L);
        s.setShiftName("x");
        s.setShiftType(1);
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(18, 0));
        service.updateShift(s);
        verify(mapper).updateById(any(AttendanceShift.class));
    }

    @Test
    @DisplayName("delete：调用 deleteById")
    void delete_ok() {
        service.deleteShift(8L);
        verify(mapper).deleteById(8L);
    }

    @Test
    @DisplayName("listEnabled / listAll / getById")
    void list_methods() {
        when(mapper.selectList(any())).thenReturn(List.of(new AttendanceShift()));
        when(mapper.selectById(any())).thenReturn(new AttendanceShift());
        assertThat(service.listEnabled()).hasSize(1);
        assertThat(service.listAll()).hasSize(1);
        assertThat(service.getById(1L)).isNotNull();
    }
}
