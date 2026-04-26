package com.pengcheng.oa.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批模板（approval_template）。
 * <p>
 * V4.0 内置 5 类：请假（leave）/外出（outing）/加班（overtime）/报销（reimburse）/通用（general），
 * 此外允许租户自定义业务编码。模板 + 流程定义共同决定一次申请怎么走。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_template")
public class ApprovalTemplate extends BaseEntity {

    /** 内置业务编码 */
    public static final String CODE_LEAVE = "leave";
    public static final String CODE_OUTING = "outing";
    public static final String CODE_OVERTIME = "overtime";
    public static final String CODE_REIMBURSE = "reimburse";
    public static final String CODE_GENERAL = "general";

    /** 业务编码（唯一） */
    private String code;

    /** 模板名称 */
    private String name;

    /** 表单 schema（JSON），前端按 schema 渲染表单 */
    private String formSchema;

    /** 默认审批流程定义 ID */
    private Long defaultFlowDefId;

    /** 模板分类：1=假勤 2=出差/外出 3=费用 4=通用 */
    private Integer category;

    /** 是否启用 */
    private Integer enabled;

    /** 备注 */
    private String remark;
}
