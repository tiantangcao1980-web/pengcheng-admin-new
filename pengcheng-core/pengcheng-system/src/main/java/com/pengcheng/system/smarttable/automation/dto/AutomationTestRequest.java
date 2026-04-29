package com.pengcheng.system.smarttable.automation.dto;

import lombok.Data;

import java.util.Map;

/**
 * 自动化规则试运行请求 DTO
 */
@Data
public class AutomationTestRequest {

    /** 模拟的触发类型（可覆盖规则原触发类型，方便测试） */
    private String triggerType;

    /** 模拟的记录 ID */
    private Long recordId;

    /** 模拟的变更后行数据 */
    private Map<String, Object> newRow;

    /** 模拟的变更前行数据（可选） */
    private Map<String, Object> oldRow;

    /** 模拟触发的字段 key（FIELD_CHANGED 触发时使用） */
    private String fieldKey;
}
