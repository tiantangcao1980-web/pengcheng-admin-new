package com.pengcheng.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 考勤地理围栏（多围栏支持）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_geo_fence")
public class GeoFence extends BaseEntity {

    /** 围栏名称 */
    private String name;
    /** 中心点经度 */
    private BigDecimal centerLng;
    /** 中心点纬度 */
    private BigDecimal centerLat;
    /** 半径（米） */
    private Integer radiusMeters;
    /** 是否启用 0=禁用 1=启用 */
    private Integer active;
    /** 扩展字段（JSON） */
    private String extra;
}
