package com.pengcheng.system.eventbus.webhook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("webhook_subscription")
public class WebhookSubscription implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String name;
    private String url;
    private String eventCodes;
    private String secret;
    private String headers;
    private Integer enabled;
    private LocalDateTime lastDeliveryAt;
    private String lastDeliveryStatus;
    private Integer failureCount;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
