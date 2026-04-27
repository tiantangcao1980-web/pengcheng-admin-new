package com.pengcheng.realty.unit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.unit.entity.RealtyUnitStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房源状态变更日志 Mapper 接口
 */
@Mapper
public interface RealtyUnitStatusLogMapper extends BaseMapper<RealtyUnitStatusLog> {
}
