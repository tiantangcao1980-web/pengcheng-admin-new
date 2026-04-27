package com.pengcheng.system.saas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.saas.entity.TenantSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface TenantSubscriptionMapper extends BaseMapper<TenantSubscription> {

    @Select("SELECT * FROM tenant_subscription WHERE tenant_id=#{tenantId} AND status IN ('TRIAL','ACTIVE') " +
            "ORDER BY end_date DESC LIMIT 1")
    TenantSubscription findActive(@Param("tenantId") Long tenantId);

    /** 批量将过期订阅置为 EXPIRED；返回受影响行数。 */
    @Update("UPDATE tenant_subscription SET status='EXPIRED', update_time=NOW() " +
            "WHERE status IN ('TRIAL','ACTIVE') AND end_date < #{today}")
    int sweepExpired(@Param("today") LocalDate today);
}
