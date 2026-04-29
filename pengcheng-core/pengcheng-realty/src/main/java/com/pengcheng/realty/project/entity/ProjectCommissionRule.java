package com.pengcheng.realty.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 项目佣金规则实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("project_commission_rule")
public class ProjectCommissionRule extends BaseEntity {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 物业类型（V17 新增 — 与 Commission.propertyType 同值）
     * 取值：RESIDENTIAL/COMMERCIAL/APARTMENT/OFFICE/OTHER；默认 RESIDENTIAL
     * 同一楼盘可有多条规则按物业类型区分
     */
    private String propertyType;

    /**
     * 客户籍贯（V17 新增 — 与 Commission.customerOrigin 同值）
     * 取值：DOMESTIC内地 / OVERSEAS境外；默认 DOMESTIC
     * 内地/境外客户佣金率往往不同
     */
    private String customerOrigin;

    /**
     * 基础佣金比例
     */
    private BigDecimal baseRate;

    /**
     * 跳点规则JSON
     */
    private String jumpPointRules;

    /**
     * 现金奖
     */
    private BigDecimal cashReward;

    /**
     * 开单奖
     */
    private BigDecimal firstDealReward;

    /**
     * 平台奖励
     */
    private BigDecimal platformReward;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 状态：1-生效 2-待审批 3-已失效
     */
    private Integer status;
}
