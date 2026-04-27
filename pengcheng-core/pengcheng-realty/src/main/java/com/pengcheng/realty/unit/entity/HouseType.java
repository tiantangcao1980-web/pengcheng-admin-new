package com.pengcheng.realty.unit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 户型实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_house_type")
public class HouseType implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 楼盘 ID */
    private Long projectId;

    /** 户型代码，如 A1/B2 */
    private String code;

    /** 户型名称 */
    private String name;

    /** 卧室数量 */
    private Integer bedrooms;

    /** 客厅数量 */
    private Integer livingRooms;

    /** 卫生间数量 */
    private Integer bathrooms;

    /** 建筑面积 m² */
    private BigDecimal area;

    /** 套内面积 */
    private BigDecimal insideArea;

    /** 朝向 */
    private String orientation;

    /** 户型图 OSS URL */
    private String layoutImage;

    /** 指导价 */
    private BigDecimal basePrice;

    /** 描述 */
    private String description;

    /** 是否启用：1-是 0-否 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
