package com.pengcheng.system.saas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.saas.entity.SaasUsageMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SaasUsageMetricMapper extends BaseMapper<SaasUsageMetric> {

    /** UPSERT 计量值（增量累加，避免并发覆盖）。 */
    @Update("INSERT INTO saas_usage_metric (tenant_id, metric_type, period_yyyymm, value_num) " +
            "VALUES (#{tenantId}, #{type}, #{period}, #{delta}) " +
            "ON DUPLICATE KEY UPDATE value_num = value_num + #{delta}, last_update = NOW()")
    int upsertIncrement(@Param("tenantId") Long tenantId,
                         @Param("type") String type,
                         @Param("period") String period,
                         @Param("delta") long delta);
}
