package com.pengcheng.hr.attendance.service;

import com.pengcheng.hr.attendance.entity.GeoFence;
import com.pengcheng.hr.attendance.mapper.GeoFenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GeoFenceCacheService 单元测试。
 * 覆盖：围栏内/围栏外、多围栏命中、空围栏、Haversine 距离正确性、refresh 重载、null 入参。
 */
@DisplayName("GeoFenceCacheService")
class GeoFenceCacheServiceTest {

    private GeoFenceMapper geoFenceMapper;
    private GeoFenceCacheService service;

    /** 北京天安门附近 */
    private static final double BJ_LNG = 116.397428;
    private static final double BJ_LAT = 39.90923;

    /** 上海人民广场 */
    private static final double SH_LNG = 121.473667;
    private static final double SH_LAT = 31.230525;

    @BeforeEach
    void setUp() {
        geoFenceMapper = mock(GeoFenceMapper.class);
        service = new GeoFenceCacheService(geoFenceMapper);
    }

    private GeoFence fence(String name, double lng, double lat, int radius) {
        return GeoFence.builder()
                .name(name)
                .centerLng(BigDecimal.valueOf(lng))
                .centerLat(BigDecimal.valueOf(lat))
                .radiusMeters(radius)
                .active(1)
                .build();
    }

    @Test
    @DisplayName("围栏内（距离 < 半径）返回 true")
    void insideFenceReturnsTrue() {
        when(geoFenceMapper.selectList(any())).thenReturn(List.of(fence("总部", BJ_LNG, BJ_LAT, 200)));
        service.refresh();

        // 偏移约 50m（纬度方向 0.00045°≈ 50m）
        assertThat(service.isInsideFence(BJ_LNG, BJ_LAT + 0.00045)).isTrue();
    }

    @Test
    @DisplayName("围栏外（距离 > 半径）返回 false")
    void outsideFenceReturnsFalse() {
        when(geoFenceMapper.selectList(any())).thenReturn(List.of(fence("总部", BJ_LNG, BJ_LAT, 200)));
        service.refresh();

        // 偏移约 1.1km（纬度方向 0.01°≈ 1.11km）
        assertThat(service.isInsideFence(BJ_LNG, BJ_LAT + 0.01)).isFalse();
    }

    @Test
    @DisplayName("多围栏：任一命中即返回 true")
    void multipleFencesAnyHitReturnsTrue() {
        when(geoFenceMapper.selectList(any())).thenReturn(List.of(
                fence("北京总部", BJ_LNG, BJ_LAT, 200),
                fence("上海分部", SH_LNG, SH_LAT, 300)
        ));
        service.refresh();

        // 在上海分部内（偏移约 50m）
        assertThat(service.isInsideFence(SH_LNG, SH_LAT + 0.00045)).isTrue();
        // 在北京总部内
        assertThat(service.isInsideFence(BJ_LNG, BJ_LAT)).isTrue();
        // 都不在
        assertThat(service.isInsideFence(113.0, 28.0)).isFalse();
    }

    @Test
    @DisplayName("空围栏列表 → 永远 false")
    void emptyFenceListAlwaysFalse() {
        when(geoFenceMapper.selectList(any())).thenReturn(Collections.emptyList());
        service.refresh();

        assertThat(service.isInsideFence(BJ_LNG, BJ_LAT)).isFalse();
        assertThat(service.isInsideFence(SH_LNG, SH_LAT)).isFalse();
        assertThat(service.snapshot()).isEmpty();
    }

    @Test
    @DisplayName("null 经纬度返回 false")
    void nullCoordinatesReturnFalse() {
        when(geoFenceMapper.selectList(any())).thenReturn(List.of(fence("总部", BJ_LNG, BJ_LAT, 200)));
        service.refresh();

        assertThat(service.isInsideFence(null, BJ_LAT)).isFalse();
        assertThat(service.isInsideFence(BJ_LNG, null)).isFalse();
        assertThat(service.isInsideFence(null, null)).isFalse();
    }

    @Test
    @DisplayName("Haversine 距离：北京 → 上海约 1067 km，容差 ±5%")
    void haversineDistanceBeijingToShanghai() {
        double meters = GeoFenceCacheService.distanceMeters(BJ_LNG, BJ_LAT, SH_LNG, SH_LAT);
        double km = meters / 1000d;
        // 实际地理距离约 1067 km；±5% 区间为 [1014, 1120]
        assertThat(km).isBetween(1014d, 1120d);
    }

    @Test
    @DisplayName("Haversine 距离：同一点为 0")
    void haversineDistanceSamePointIsZero() {
        double meters = GeoFenceCacheService.distanceMeters(BJ_LNG, BJ_LAT, BJ_LNG, BJ_LAT);
        assertThat(meters).isEqualTo(0d);
    }

    @Test
    @DisplayName("refresh 后围栏列表被替换为最新数据")
    void refreshReloadsLatestData() {
        when(geoFenceMapper.selectList(any())).thenReturn(List.of(fence("旧", BJ_LNG, BJ_LAT, 200)));
        service.refresh();
        assertThat(service.snapshot()).hasSize(1);

        when(geoFenceMapper.selectList(any())).thenReturn(List.of(
                fence("新A", BJ_LNG, BJ_LAT, 100),
                fence("新B", SH_LNG, SH_LAT, 100)
        ));
        service.refresh();
        assertThat(service.snapshot()).hasSize(2);
        assertThat(service.snapshot()).extracting(GeoFence::getName).containsExactly("新A", "新B");
    }
}
