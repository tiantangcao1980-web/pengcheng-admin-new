package com.pengcheng.message.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 三通道推送请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelPushRequest {

    private String title;
    private String content;

    /** 业务类型（必填，用于路由前端跳转 + 日志聚合） */
    private String bizType;

    /** 业务 ID */
    private Long bizId;

    /** 小程序订阅消息模板 ID（不填则不会用 MP 通道） */
    private String subscribeTemplateId;

    /** 订阅消息字段值 */
    private Map<String, String> subscribeData;

    /** 点击跳转的小程序页面（可空） */
    private String subscribePage;

    /** APP 透传扩展 */
    private Map<String, String> extras;

    public static ChannelPushRequest of(String bizType, Long bizId) {
        return ChannelPushRequest.builder().bizType(bizType).bizId(bizId).build();
    }
}
