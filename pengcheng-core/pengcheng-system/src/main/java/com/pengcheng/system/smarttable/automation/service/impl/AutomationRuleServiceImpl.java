package com.pengcheng.system.smarttable.automation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.smarttable.automation.entity.SmartTableAutomationRule;
import com.pengcheng.system.smarttable.automation.mapper.SmartTableAutomationRuleMapper;
import com.pengcheng.system.smarttable.automation.service.AutomationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 自动化规则服务实现
 */
@Service
@RequiredArgsConstructor
public class AutomationRuleServiceImpl implements AutomationRuleService {

    private final SmartTableAutomationRuleMapper ruleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableAutomationRule create(SmartTableAutomationRule rule) {
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        ruleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmartTableAutomationRule update(SmartTableAutomationRule rule) {
        ruleMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public SmartTableAutomationRule getById(Long id) {
        return ruleMapper.selectById(id);
    }

    @Override
    public Page<SmartTableAutomationRule> pageByTable(Long tableId, int page, int pageSize) {
        LambdaQueryWrapper<SmartTableAutomationRule> wrapper = new LambdaQueryWrapper<SmartTableAutomationRule>()
                .eq(SmartTableAutomationRule::getTableId, tableId)
                .orderByDesc(SmartTableAutomationRule::getCreateTime);
        return ruleMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<SmartTableAutomationRule> listEnabledByTable(Long tableId) {
        return ruleMapper.selectList(new LambdaQueryWrapper<SmartTableAutomationRule>()
                .eq(SmartTableAutomationRule::getTableId, tableId)
                .eq(SmartTableAutomationRule::getEnabled, 1)
                .orderByAsc(SmartTableAutomationRule::getId));
    }

    @Override
    public List<SmartTableAutomationRule> listByTrigger(Long tableId, String triggerType) {
        return ruleMapper.selectList(new LambdaQueryWrapper<SmartTableAutomationRule>()
                .eq(SmartTableAutomationRule::getTableId, tableId)
                .eq(SmartTableAutomationRule::getEnabled, 1)
                .eq(SmartTableAutomationRule::getTriggerType, triggerType)
                .orderByAsc(SmartTableAutomationRule::getId));
    }
}
