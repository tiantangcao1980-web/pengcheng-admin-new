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
 * 房源实体（一栋楼一层一户的颗粒度）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_unit")
public class RealtyUnit implements Serializable {

    // ---- 状态常量 ----
    public static final String STATUS_AVAILABLE   = "AVAILABLE";
    public static final String STATUS_RESERVED    = "RESERVED";
    public static final String STATUS_SUBSCRIBED  = "SUBSCRIBED";
    public static final String STATUS_SIGNED      = "SIGNED";
    public static final String STATUS_SOLD        = "SOLD";
    public static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 楼盘 ID */
    private Long projectId;

    /** 户型 ID */
    private Long houseTypeId;

    /** 楼栋号 */
    private String building;

    /** 楼层 */
    private Integer floor;

    /** 房号，如 0301 */
    private String unitNo;

    /** 完整编号，如 1-3-0301（building-floor-unitNo） */
    private String fullNo;

    /** 建筑面积 m² */
    private BigDecimal area;

    /** 挂牌价 */
    private BigDecimal listPrice;

    /** 实际成交价 */
    private BigDecimal actualPrice;

    /** 状态：AVAILABLE/RESERVED/SUBSCRIBED/SIGNED/SOLD/UNAVAILABLE */
    private String status;

    /** 锁定人 */
    private Long lockedBy;

    /** 锁定到期时间 */
    private LocalDateTime lockedUntil;

    /** 当前关联客户 */
    private Long customerId;

    /** 关联成交单 */
    private Long dealId;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
