package com.pengcheng.system.eventbus.webhook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("webhook_delivery")
public class WebhookDelivery implements Serializable {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DEAD = "DEAD";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long subscriptionId;
    private Long tenantId;
    private String eventCode;
    private String eventId;
    private String payload;
    private String requestUrl;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private Integer responseStatus;
    private String responseBody;
    private String errorMsg;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
