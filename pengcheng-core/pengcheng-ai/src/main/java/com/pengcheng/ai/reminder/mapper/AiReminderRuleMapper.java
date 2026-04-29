package com.pengcheng.ai.reminder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.ai.reminder.entity.AiReminderRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 智能提醒调度规则 Mapper
 */
@Mapper
public interface AiReminderRuleMapper extends BaseMapper<AiReminderRule> {
}
