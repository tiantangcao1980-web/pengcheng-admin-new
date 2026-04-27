package com.pengcheng.realty.unit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 房源状态变更日志（审计）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_unit_status_log")
public class RealtyUnitStatusLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 房源 ID */
    private Long unitId;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作人 */
    private Long operatorId;

    /** 关联客户 */
    private Long customerId;

    /** 关联成交单 */
    private Long dealId;

    /** 变更原因 */
    private String reason;

    private LocalDateTime createTime;
}
