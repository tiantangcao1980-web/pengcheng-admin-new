package com.pengcheng.system.smarttable.automation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 多维表格自动化规则
 */
@Data
@TableName("smart_table_automation_rule")
public class SmartTableAutomationRule implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tableId;

    private String name;

    /** 1=启用，0=禁用 */
    private Integer enabled;

    /**
     * 触发类型：RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/FIELD_CHANGED/SCHEDULED
     */
    private String triggerType;

    /**
     * 触发配置 JSON：{fieldKey?: string, when?: string(cron)}
     */
    private String triggerConfig;

    /**
     * 条件 DSL JSON（null 表示无条件）
     */
    private String conditionJson;

    /**
     * 动作列表 JSON：[{type, params}, ...]
     * type ∈ CREATE_TODO/SEND_EMAIL/UPDATE_RECORD/CALL_WEBHOOK/SEND_NOTIFICATION
     */
    private String actionsJson;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
