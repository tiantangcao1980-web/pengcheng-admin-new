package com.pengcheng.system.smarttable.automation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationLog;
import com.pengcheng.system.smarttable.automation.mapper.SmartTableAutomationLogMapper;
import com.pengcheng.system.smarttable.automation.service.AutomationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 自动化执行日志服务实现
 */
@Service
@RequiredArgsConstructor
public class AutomationLogServiceImpl implements AutomationLogService {

    private final SmartTableAutomationLogMapper logMapper;

    @Override
    public void save(SmartTableAutomationLog log) {
        logMapper.insert(log);
    }

    @Override
    public Page<SmartTableAutomationLog> page(Long ruleId, Long tableId, int page, int pageSize) {
        LambdaQueryWrapper<SmartTableAutomationLog> wrapper = new LambdaQueryWrapper<>();
        if (ruleId != null) {
            wrapper.eq(SmartTableAutomationLog::getRuleId, ruleId);
        }
        if (tableId != null) {
            wrapper.eq(SmartTableAutomationLog::getTableId, tableId);
        }
        wrapper.orderByDesc(SmartTableAutomationLog::getCreateTime);
        return logMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
