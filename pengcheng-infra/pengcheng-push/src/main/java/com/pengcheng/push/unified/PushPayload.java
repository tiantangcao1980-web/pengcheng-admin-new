package com.pengcheng.push.unified;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 统一推送消息载荷
 *
 * <p>同时承载 APP / 订阅消息 / 站内信 三个通道所需字段：
 * <ul>
 *     <li>APP / 站内信：title + content</li>
 *     <li>订阅消息：subscribeTemplateId + subscribeData + subscribePage</li>
 *     <li>业务路由：bizType + bizId</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushPayload {

    private String title;
    private String content;

    /** 业务类型（customer / approval / payment ...） */
    private String bizType;

    /** 业务 ID（用于站内信跳转和 trace 关联） */
    private Long bizId;

    /** 小程序订阅消息模板 ID */
    private String subscribeTemplateId;

    /** 订阅消息字段值（key 为模板字段名） */
    private Map<String, String> subscribeData;

    /** 点击跳转的小程序页面，可空 */
    private String subscribePage;

    /** 推送扩展（透传到 APP 通道） */
    private Map<String, String> extras;
}
