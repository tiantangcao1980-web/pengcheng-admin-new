package com.pengcheng.finance.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.finance.contract.entity.ContractVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同版本历史 Mapper。
 */
@Mapper
public interface ContractVersionMapper extends BaseMapper<ContractVersion> {
}
