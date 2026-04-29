package com.pengcheng.realty.sop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.sop.entity.RealtyVisitSop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 带看 SOP Mapper
 */
@Mapper
public interface RealtyVisitSopMapper extends BaseMapper<RealtyVisitSop> {

    /**
     * 风控查询：查找指定客户在指定时间点之前（带看时间 <= time）且尚未过期（expires_at > time）
     * 且属于指定渠道的 SOP 记录（status=CONFIRMED）。
     * <p>
     * 用于成交分佣前置校验：确认该客户确实通过该渠道带看过。
     */
    @Select("SELECT * FROM realty_visit_sop " +
            "WHERE customer_id = #{customerId} " +
            "  AND alliance_id = #{allianceId} " +
            "  AND status = 'CONFIRMED' " +
            "  AND visit_time <= #{time} " +
            "  AND expires_at > #{time} " +
            "LIMIT 1")
    RealtyVisitSop findCoveredSop(@Param("customerId") Long customerId,
                                  @Param("allianceId") Long allianceId,
                                  @Param("time") LocalDateTime time);

    /**
     * 查询客户在某渠道的所有有效（PENDING_CONFIRM/CONFIRMED）带看记录
     */
    @Select("SELECT * FROM realty_visit_sop " +
            "WHERE customer_id = #{customerId} " +
            "  AND alliance_id = #{allianceId} " +
            "  AND status IN ('PENDING_CONFIRM', 'CONFIRMED') " +
            "ORDER BY visit_time DESC")
    List<RealtyVisitSop> listActiveSops(@Param("customerId") Long customerId,
                                        @Param("allianceId") Long allianceId);
}
