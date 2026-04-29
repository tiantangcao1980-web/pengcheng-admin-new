package com.pengcheng.system.eventbus.webhook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.eventbus.webhook.entity.WebhookSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WebhookSubscriptionMapper extends BaseMapper<WebhookSubscription> {

    /**
     * 查找订阅了指定 event 的所有 enabled=1 订阅
     * （用 LIKE %code% 简化；生产建议用关联表 + UNIQUE INDEX）
     */
    @Select("SELECT * FROM webhook_subscription " +
            "WHERE enabled = 1 " +
            "  AND tenant_id = #{tenantId} " +
            "  AND FIND_IN_SET(#{eventCode}, REPLACE(event_codes, ' ', '')) > 0")
    List<WebhookSubscription> findEnabledByEvent(@Param("tenantId") Long tenantId,
                                                  @Param("eventCode") String eventCode);
}
