package com.pengcheng.system.eventbus.webhook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("webhook_event_type")
public class WebhookEventType implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;
    private String name;
    private String category;
    private String payloadSchema;
    private Integer enabled;
    private LocalDateTime createTime;
}
