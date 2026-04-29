package com.pengcheng.realty.pipeline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售商机
 *
 * 一个客户在某楼盘上的销售机会，含当前阶段、预期金额、负责人。
 * 阶段流转记录在 OpportunityStageLog。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("realty_opportunity")
public class Opportunity extends BaseEntity {

    private Long customerId;

    private Long projectId;

    /** 当前阶段ID */
    private Long stageId;

    /** 商机标题（默认 客户名+楼盘名） */
    private String title;

    /** 预期成交金额 */
    private BigDecimal expectedAmount;

    /** 预期关闭日期 */
    private LocalDate expectedCloseDate;

    /** 当前负责人（业务员） */
    private Long ownerId;

    /** 下一步动作 */
    private String nextAction;

    /** 下一步动作时间 */
    private LocalDateTime nextActionAt;

    /** 流失原因（仅 LOST 状态填） */
    private String lostReason;

    /** 最近一次阶段变更时间 */
    private LocalDateTime lastStageChangedAt;
}
