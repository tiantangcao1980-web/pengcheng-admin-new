package com.pengcheng.realty.sop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.sop.entity.RealtyCommissionTripartite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 佣金三方协议 Mapper
 */
@Mapper
public interface RealtyCommissionTripartiteMapper extends BaseMapper<RealtyCommissionTripartite> {

    /**
     * 按 deal_id 查询（UNIQUE 字段，快速幂等判断）
     */
    @Select("SELECT * FROM realty_commission_tripartite WHERE deal_id = #{dealId} LIMIT 1")
    RealtyCommissionTripartite selectByDealId(@Param("dealId") Long dealId);

    /**
     * 按 sign_flow_id 查询（Webhook 回调时用）
     */
    @Select("SELECT * FROM realty_commission_tripartite WHERE sign_flow_id = #{signFlowId} LIMIT 1")
    RealtyCommissionTripartite selectBySignFlowId(@Param("signFlowId") String signFlowId);
}
