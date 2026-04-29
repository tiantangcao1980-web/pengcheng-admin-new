package com.pengcheng.realty.customer.field.service;

import com.pengcheng.realty.customer.field.dto.FieldVisitCheckInDTO;
import com.pengcheng.realty.customer.field.dto.FieldVisitCheckOutDTO;
import com.pengcheng.realty.customer.field.entity.FieldVisit;
import com.pengcheng.realty.customer.field.event.FieldVisitEvent;
import com.pengcheng.realty.customer.field.mapper.FieldVisitMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售外勤拜访服务单测
 */
@DisplayName("FieldVisitService — 销售外勤")
class FieldVisitServiceTest {

    private FieldVisitMapper mapper;
    private ApplicationEventPublisher publisher;
    private FieldVisitService service;

    @BeforeEach
    void setUp() {
        mapper = mock(FieldVisitMapper.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new FieldVisitService(mapper, publisher);
    }

    private FieldVisitCheckInDTO baseCheckIn() {
        return FieldVisitCheckInDTO.builder()
                .userId(1L).customerId(100L)
                .longitude(new BigDecimal("116.397428"))
                .latitude(new BigDecimal("39.90923"))
                .address("北京市天安门")
                .photoUrls("https://oss/p1.jpg")
                .purpose("跟进客户")
                .build();
    }

    @Test
    @DisplayName("签到：必填 userId")
    void checkIn_missingUser() {
        FieldVisitCheckInDTO dto = baseCheckIn();
        dto.setUserId(null);
        assertThatThrownBy(() -> service.checkIn(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务员");
    }

    @Test
    @DisplayName("签到：必须关联客户或楼盘")
    void checkIn_missingTarget() {
        FieldVisitCheckInDTO dto = baseCheckIn();
        dto.setCustomerId(null);
        dto.setProjectId(null);
        assertThatThrownBy(() -> service.checkIn(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("客户或楼盘");
    }

    @Test
    @DisplayName("签到：必填 GPS")
    void checkIn_missingGps() {
        FieldVisitCheckInDTO dto = baseCheckIn();
        dto.setLongitude(null);
        assertThatThrownBy(() -> service.checkIn(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GPS");
    }

    @Test
    @DisplayName("签到：默认 visitType=1（客户拜访）+ 发事件")
    void checkIn_defaultsAndEvent() {
        service.checkIn(baseCheckIn());

        ArgumentCaptor<FieldVisit> visitCap = ArgumentCaptor.forClass(FieldVisit.class);
        verify(mapper).insert(visitCap.capture());
        assertThat(visitCap.getValue().getVisitType()).isEqualTo(FieldVisit.TYPE_CUSTOMER_VISIT);
        assertThat(visitCap.getValue().getCheckInTime()).isNotNull();

        ArgumentCaptor<FieldVisitEvent> evtCap = ArgumentCaptor.forClass(FieldVisitEvent.class);
        verify(publisher).publishEvent(evtCap.capture());
        assertThat(evtCap.getValue().getAction()).isEqualTo(FieldVisitEvent.ACTION_CHECK_IN);
        assertThat(evtCap.getValue().getCustomerId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("签退：写入结果 + 计算时长 + 发事件")
    void checkOut_succeeds() {
        FieldVisit visit = FieldVisit.builder()
                .userId(1L).customerId(100L)
                .checkInTime(LocalDateTime.now().minusMinutes(45))
                .build();
        visit.setId(500L);
        when(mapper.selectById(500L)).thenReturn(visit);

        service.checkOut(FieldVisitCheckOutDTO.builder()
                .fieldVisitId(500L).userId(1L).result("客户有意向").build());

        assertThat(visit.getCheckOutTime()).isNotNull();
        assertThat(visit.getResult()).isEqualTo("客户有意向");
        assertThat(visit.getDurationMinutes()).isBetween(40, 50);

        verify(publisher).publishEvent(any(FieldVisitEvent.class));
    }

    @Test
    @DisplayName("签退：补充照片合并到现有列表")
    void checkOut_appendPhotos() {
        FieldVisit visit = FieldVisit.builder()
                .userId(1L).customerId(100L)
                .checkInTime(LocalDateTime.now().minusMinutes(10))
                .photoUrls("https://oss/in.jpg")
                .build();
        visit.setId(500L);
        when(mapper.selectById(500L)).thenReturn(visit);

        service.checkOut(FieldVisitCheckOutDTO.builder()
                .fieldVisitId(500L).userId(1L)
                .additionalPhotoUrls("https://oss/out.jpg").build());

        assertThat(visit.getPhotoUrls()).contains("in.jpg").contains("out.jpg");
    }

    @Test
    @DisplayName("签退：已签退不可重复")
    void checkOut_alreadyCheckedOut() {
        FieldVisit visit = FieldVisit.builder()
                .userId(1L)
                .checkInTime(LocalDateTime.now().minusHours(1))
                .checkOutTime(LocalDateTime.now())
                .build();
        visit.setId(500L);
        when(mapper.selectById(500L)).thenReturn(visit);

        assertThatThrownBy(() -> service.checkOut(FieldVisitCheckOutDTO.builder()
                .fieldVisitId(500L).userId(1L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已签退");
    }

    @Test
    @DisplayName("签退：仅本人可签退")
    void checkOut_otherUserRejected() {
        FieldVisit visit = FieldVisit.builder()
                .userId(1L)
                .checkInTime(LocalDateTime.now().minusMinutes(30))
                .build();
        visit.setId(500L);
        when(mapper.selectById(500L)).thenReturn(visit);

        assertThatThrownBy(() -> service.checkOut(FieldVisitCheckOutDTO.builder()
                .fieldVisitId(500L).userId(99L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("本人");
    }

    @Test
    @DisplayName("签退：记录不存在")
    void checkOut_notFound() {
        when(mapper.selectById(any())).thenReturn(null);
        assertThatThrownBy(() -> service.checkOut(FieldVisitCheckOutDTO.builder()
                .fieldVisitId(999L).userId(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("countByUserInMonth: null 安全降级 0")
    void countByUserInMonth_nullSafe() {
        when(mapper.selectCount(any())).thenReturn(null);
        assertThat(service.countByUserInMonth(1L, 2026, 4)).isEqualTo(0);
    }

    @Test
    @DisplayName("countByUserInMonth: 返回正常计数")
    void countByUserInMonth_returnsCount() {
        when(mapper.selectCount(any())).thenReturn(15L);
        assertThat(service.countByUserInMonth(1L, 2026, 4)).isEqualTo(15);
    }

    @Test
    @DisplayName("仅楼盘踏勘场景（无 customerId）允许签到")
    void checkIn_projectOnly() {
        FieldVisitCheckInDTO dto = baseCheckIn();
        dto.setCustomerId(null);
        dto.setProjectId(2001L);
        dto.setVisitType(FieldVisit.TYPE_PROJECT_INSPECT);

        service.checkIn(dto);

        ArgumentCaptor<FieldVisit> cap = ArgumentCaptor.forClass(FieldVisit.class);
        verify(mapper).insert(cap.capture());
        assertThat(cap.getValue().getProjectId()).isEqualTo(2001L);
        assertThat(cap.getValue().getCustomerId()).isNull();
    }

    @Test
    @DisplayName("listByUserAndDate / listByCustomer 走 mapper")
    void listMethods() {
        when(mapper.selectList(any())).thenReturn(java.util.List.of(
                FieldVisit.builder().userId(1L).build()));

        assertThat(service.listByUserAndDate(1L, java.time.LocalDate.now())).hasSize(1);
        assertThat(service.listByCustomer(100L)).hasSize(1);
        verify(mapper, times(2)).selectList(any());
    }
}
