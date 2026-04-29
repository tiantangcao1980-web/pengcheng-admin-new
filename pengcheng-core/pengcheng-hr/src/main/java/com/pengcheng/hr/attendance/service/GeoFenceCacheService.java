package com.pengcheng.hr.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.attendance.entity.GeoFence;
import com.pengcheng.hr.attendance.mapper.GeoFenceMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 地理围栏内存缓存服务。
 *
 * <p>启动时全量加载 active=1 的围栏到内存；
 * 管理员对围栏做增删改后调 {@link #refresh()} 重新加载。
 *
 * <p>判定算法采用 Haversine 公式（地球半径 6371000 米），
 * 任一围栏命中（距离 ≤ 半径）即视为内勤。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoFenceCacheService {

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final GeoFenceMapper geoFenceMapper;

    /** 内存中已启用的围栏快照；CopyOnWrite 保证读多写少场景下的并发安全。 */
    private final List<GeoFence> activeFences = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 重新加载所有 active=1 的围栏到内存。
     * 管理员维护围栏（增删改）后必须调用。
     */
    public void refresh() {
        try {
            LambdaQueryWrapper<GeoFence> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GeoFence::getActive, 1);
            List<GeoFence> fences = geoFenceMapper.selectList(wrapper);
            activeFences.clear();
            if (fences != null && !fences.isEmpty()) {
                activeFences.addAll(fences);
            }
            log.info("[GeoFenceCache] reloaded fences, size={}", activeFences.size());
        } catch (Exception e) {
            // 容忍空表/启动期 mapper 暂未就绪，避免阻塞主流程
            log.warn("[GeoFenceCache] reload failed: {}", e.getMessage());
        }
    }

    /**
     * 判断给定坐标是否在任一启用围栏内。
     *
     * @param lng 经度
     * @param lat 纬度
     * @return true=内勤（围栏内）；false=外勤（围栏外或参数缺失）
     */
    public boolean isInsideFence(Double lng, Double lat) {
        if (lng == null || lat == null) return false;
        if (activeFences.isEmpty()) return false;
        for (GeoFence fence : activeFences) {
            if (fence.getCenterLng() == null || fence.getCenterLat() == null
                    || fence.getRadiusMeters() == null) {
                continue;
            }
            double centerLng = toDouble(fence.getCenterLng());
            double centerLat = toDouble(fence.getCenterLat());
            double distance = distanceMeters(lng, lat, centerLng, centerLat);
            if (distance <= fence.getRadiusMeters()) {
                return true;
            }
        }
        return false;
    }

    /** 当前缓存的围栏快照（只读副本，便于测试与诊断） */
    public List<GeoFence> snapshot() {
        return Collections.unmodifiableList(activeFences);
    }

    /**
     * Haversine 公式计算两点间球面距离（米）。
     *
     * @param lng1 点1 经度
     * @param lat1 点1 纬度
     * @param lng2 点2 经度
     * @param lat2 点2 纬度
     * @return 距离（米）
     */
    static double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static double toDouble(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }
}
