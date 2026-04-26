package com.pengcheng.message.subscribe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 小程序订阅消息发送请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeMessageRequest {

    private String openId;

    /** 业务类型，用于查模板 */
    private String bizType;

    /** 业务事件 */
    private String eventCode;

    /** 业务字段（待渲染） */
    private Map<String, String> bizFields;

    /** 跳转页面（覆盖模板默认页面，可空） */
    private String page;
}
