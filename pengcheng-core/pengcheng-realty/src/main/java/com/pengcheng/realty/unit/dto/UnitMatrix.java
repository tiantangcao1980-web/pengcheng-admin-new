package com.pengcheng.realty.unit.dto;

import com.pengcheng.realty.unit.entity.RealtyUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 楼栋 × 楼层 × 房间二维矩阵（前端房源状态图用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitMatrix {

    /** 楼栋号 */
    private String building;

    /** 楼层列表（按楼层从高到低排列） */
    private List<FloorRow> floors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FloorRow {

        /** 楼层 */
        private Integer floor;

        /** 该楼层所有房源（按 unitNo 排序） */
        private List<RealtyUnit> units;
    }
}
