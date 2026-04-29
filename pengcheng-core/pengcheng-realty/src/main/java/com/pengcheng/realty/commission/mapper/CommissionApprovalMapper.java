package com.pengcheng.realty.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.commission.entity.CommissionApproval;
import org.apache.ibatis.annotations.Mapper;

/**
 * 佣金审批节点记录 Mapper 接口
 */
@Mapper
public interface CommissionApprovalMapper extends BaseMapper<CommissionApproval> {
}
