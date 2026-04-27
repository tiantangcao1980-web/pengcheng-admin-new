package com.pengcheng.finance.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.finance.commission.entity.CommissionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用提成记录 Mapper。
 */
@Mapper
public interface CommissionRecordMapper extends BaseMapper<CommissionRecord> {
}
