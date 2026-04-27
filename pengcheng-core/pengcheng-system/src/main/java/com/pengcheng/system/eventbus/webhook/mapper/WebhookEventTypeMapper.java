package com.pengcheng.system.eventbus.webhook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.eventbus.webhook.entity.WebhookEventType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WebhookEventTypeMapper extends BaseMapper<WebhookEventType> {
}
