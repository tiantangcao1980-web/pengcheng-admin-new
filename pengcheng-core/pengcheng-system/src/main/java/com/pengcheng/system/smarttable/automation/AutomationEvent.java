package com.pengcheng.system.smarttable.automation;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 自动化触发事件上下文
 * 由业务层（SmartTableService 等）构建并传递给 AutomationDispatcher
 */
@Data
@Builder
public class AutomationEvent {

    /** 表格 ID */
    private Long tableId;

    /** 触发记录 ID（SCHEDULED 触发时可为 null） */
    private Long recordId;

    /** 触发类型 */
    private AutomationTriggerType triggerType;

    /**
     * 变更前行数据（key=fieldKey, value=字段值）
     * RECORD_CREATED / SCHEDULED 时为 null
     */
    private Map<String, Object> oldRow;

    /**
     * 变更后行数据（key=fieldKey, value=字段值）
     * RECORD_DELETED 时为 null
     */
    private Map<String, Object> newRow;

    /**
     * 变更的字段 key（FIELD_CHANGED 触发时有值）
     */
    private String fieldKey;

    /** 是否 dry-run（试运行，不真正执行 action 副作用） */
    @Builder.Default
    private boolean dryRun = false;
}
