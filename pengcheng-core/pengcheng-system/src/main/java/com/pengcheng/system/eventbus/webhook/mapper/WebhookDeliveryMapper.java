package com.pengcheng.system.eventbus.webhook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.eventbus.webhook.entity.WebhookDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WebhookDeliveryMapper extends BaseMapper<WebhookDelivery> {

    /** 扫描可重试的投递（PENDING + next_attempt_at <= now + attempt_count < 5）。 */
    @Select("SELECT * FROM webhook_delivery " +
            "WHERE status = 'PENDING' " +
            "  AND attempt_count < 5 " +
            "  AND (next_attempt_at IS NULL OR next_attempt_at <= #{now}) " +
            "ORDER BY id ASC LIMIT #{limit}")
    List<WebhookDelivery> findRetryable(@Param("now") LocalDateTime now,
                                         @Param("limit") int limit);
}
