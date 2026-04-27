package com.pengcheng.system.smarttable.automation.dto;

import lombok.Data;

/**
 * 创建 / 更新自动化规则请求 DTO
 */
@Data
public class AutomationRuleRequest {

    private Long tableId;

    private String name;

    /** 1=启用，0=禁用 */
    private Integer enabled;

    /** RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/FIELD_CHANGED/SCHEDULED */
    private String triggerType;

    /** JSON 字符串：{fieldKey?, when?: cron} */
    private String triggerConfig;

    /** 条件 DSL JSON（可为 null 表示无条件） */
    private String conditionJson;

    /** 动作列表 JSON：[{type, params}, ...] */
    private String actionsJson;
}
