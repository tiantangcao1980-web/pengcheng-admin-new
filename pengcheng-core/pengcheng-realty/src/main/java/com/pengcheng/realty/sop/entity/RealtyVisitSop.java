package com.pengcheng.realty.sop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 带看 SOP 实例实体（每次客户带看记录）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_visit_sop")
public class RealtyVisitSop {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户 ID */
    private Long customerId;

    /** 楼盘 ID */
    private Long projectId;

    /** 陪同销售人员 ID */
    private Long salespersonId;

    /** 渠道联盟商 ID（可为 null） */
    private Long allianceId;

    /** 带看时间 */
    private LocalDateTime visitTime;

    /** 主推房源 ID（可为 null） */
    private Long visitUnitId;

    /** 带看时长（分钟） */
    private Integer durationMin;

    /**
     * 状态：
     * PENDING_CONFIRM — 待确认（已发送签署）
     * CONFIRMED       — 已确认（签署完成）
     * EXPIRED         — 已过期（超过有效期）
     * CANCELLED       — 已取消
     */
    private String status;

    /** 带看确认书文档 URL（HTML/PDF，生成后写入） */
    private String confirmDocUrl;

    /** e签宝签署流 ID */
    private String confirmSignId;

    /** 签署确认时间 */
    private LocalDateTime confirmedAt;

    /** 确认书有效期（默认 14 天） */
    private LocalDateTime expiresAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
