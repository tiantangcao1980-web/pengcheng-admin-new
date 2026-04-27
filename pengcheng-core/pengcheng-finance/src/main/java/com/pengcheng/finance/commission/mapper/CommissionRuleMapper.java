package com.pengcheng.finance.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.finance.commission.entity.CommissionRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用提成规则 Mapper。
 */
@Mapper
public interface CommissionRuleMapper extends BaseMapper<CommissionRule> {
}
