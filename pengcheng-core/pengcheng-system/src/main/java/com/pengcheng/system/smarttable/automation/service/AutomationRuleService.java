package com.pengcheng.system.smarttable.automation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationRule;

import java.util.List;

/**
 * 自动化规则服务接口
 */
public interface AutomationRuleService {

    /** 创建规则 */
    SmartTableAutomationRule create(SmartTableAutomationRule rule);

    /** 更新规则 */
    SmartTableAutomationRule update(SmartTableAutomationRule rule);

    /** 删除规则 */
    void delete(Long id);

    /** 按 ID 查询 */
    SmartTableAutomationRule getById(Long id);

    /** 分页查询某表格的规则 */
    Page<SmartTableAutomationRule> pageByTable(Long tableId, int page, int pageSize);

    /** 查询某表格所有已启用规则 */
    List<SmartTableAutomationRule> listEnabledByTable(Long tableId);

    /**
     * 查询满足触发类型的已启用规则
     *
     * @param tableId     表格 ID
     * @param triggerType 触发类型字符串
     */
    List<SmartTableAutomationRule> listByTrigger(Long tableId, String triggerType);
}
