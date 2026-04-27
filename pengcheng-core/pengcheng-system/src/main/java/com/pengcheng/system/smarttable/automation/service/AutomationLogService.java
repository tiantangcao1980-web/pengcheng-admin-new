package com.pengcheng.system.smarttable.automation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationLog;

/**
 * 自动化执行日志服务接口
 */
public interface AutomationLogService {

    /** 写入一条日志 */
    void save(SmartTableAutomationLog log);

    /**
     * 分页查询日志
     *
     * @param ruleId  规则 ID（可为 null，不过滤）
     * @param tableId 表格 ID（可为 null，不过滤）
     * @param page    页码（1 起）
     * @param pageSize 每页条数
     */
    Page<SmartTableAutomationLog> page(Long ruleId, Long tableId, int page, int pageSize);
}
