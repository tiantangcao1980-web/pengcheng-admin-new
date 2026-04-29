package com.pengcheng.finance.commission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 通用销售提成规则实体（commission_rule）。
 * <p>
 * 适用于非房产行业的通用销售提成规则，支持固定比例、阶梯比例、团队分成、DSL 表达式四种模式。
 * <p>
 * <b>边界说明</b>：房产行业专属提成（跳点/项目级别奖励等）继续使用
 * {@code realty} 模块的 {@code project_commission_rule} 表；
 * 本表仅管理跨行业通用规则。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("commission_rule")
public class CommissionRule extends BaseEntity {

    // ==================== 计算模式常量 ====================
    public static final int CALC_MODE_FIXED   = 1; // 固定比例
    public static final int CALC_MODE_LADDER  = 2; // 阶梯比例
    public static final int CALC_MODE_TEAM    = 3; // 团队分成
    public static final int CALC_MODE_DSL     = 4; // DSL 表达式

    /** 规则名称 */
    private String ruleName;

    /** 适用业务类型，如 crm_deal / general */
    private String bizType;

    /**
     * 计算模式。
     *
     * @see #CALC_MODE_FIXED
     * @see #CALC_MODE_DSL
     */
    private Integer calcMode;

    /** 固定比例（calcMode=1 时有效，如 0.0300 = 3%） */
    private BigDecimal rate;

    /**
     * 阶梯配置 JSON（calcMode=2）。
     * 示例：{@code [{"min":0,"max":100000,"rate":0.03},{"min":100000,"max":null,"rate":0.05}]}
     */
    private String ladderJson;

    /**
     * 团队分成配置 JSON（calcMode=3）。
     * 示例：{@code [{"userId":1,"ratio":0.6},{"userId":2,"ratio":0.4}]}
     */
    private String teamSplitJson;

    /**
     * DSL 表达式（calcMode=4）。
     * 格式由后续规则引擎工单定义，此处仅做文本存储。
     */
    private String expressionDsl;

    /** 是否启用：1=启用 0=停用 */
    private Integer active;

    /** 规则说明 */
    private String remark;
}
