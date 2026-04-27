package com.pengcheng.system.smarttable.automation;

/**
 * 自动化触发类型
 */
public enum AutomationTriggerType {

    /** 新记录创建时触发 */
    RECORD_CREATED,

    /** 记录任意字段更新时触发 */
    RECORD_UPDATED,

    /** 记录删除时触发 */
    RECORD_DELETED,

    /** 指定字段值变化时触发（需配合 trigger_config.fieldKey） */
    FIELD_CHANGED,

    /** 定时触发（需配合 trigger_config.when: cron 表达式） */
    SCHEDULED;

    public static AutomationTriggerType of(String value) {
        for (AutomationTriggerType t : values()) {
            if (t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知触发类型: " + value);
    }
}
