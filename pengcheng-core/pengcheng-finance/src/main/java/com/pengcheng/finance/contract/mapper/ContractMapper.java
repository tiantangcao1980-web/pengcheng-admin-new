package com.pengcheng.finance.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.finance.contract.entity.Contract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同主表 Mapper。
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {
}
