package com.pengcheng.system.smarttable.automation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 多维表格自动化执行日志
 */
@Data
@TableName("smart_table_automation_log")
public class SmartTableAutomationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Long tableId;

    /** 触发记录 ID（定时触发时可为 null） */
    private Long recordId;

    private String triggerType;

    /** 1=成功，0=失败 */
    private Integer success;

    private Integer actionsCount;

    private String errorMsg;

    private LocalDateTime createTime;
}
