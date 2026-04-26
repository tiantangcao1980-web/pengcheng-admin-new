package com.pengcheng.message.subscribe;

import com.pengcheng.push.unified.ChannelSubscribeSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * 小程序订阅消息统一调度 Service
 *
 * <p>流程：
 * <ol>
 *     <li>按 bizType + eventCode 查模板（缓存 + DB）</li>
 *     <li>{@link SubscribeMessageRenderer} 渲染字段</li>
 *     <li>通过 {@link ChannelSubscribeSender} 调用微信接口</li>
 * </ol>
 */
@Slf4j
public class SubscribeMessageService {

    private final SubscribeMsgTemplateRepository repository;
    private final ChannelSubscribeSender sender;

    public SubscribeMessageService(SubscribeMsgTemplateRepository repository,
                                   ChannelSubscribeSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    /**
     * 发送订阅消息
     *
     * @return 是否成功（找不到启用的模板返回 false）
     */
    public boolean send(SubscribeMessageRequest request) {
        if (request == null || request.getOpenId() == null || request.getOpenId().isBlank()) {
            log.warn("SubscribeMessageService.send invalid request: missing openId");
            return false;
        }
        Optional<SubscribeMsgTemplate> templateOpt =
                repository.findEnabled(request.getBizType(), request.getEventCode());
        if (templateOpt.isEmpty()) {
            log.warn("SubscribeMessageService template not found: bizType={}, event={}",
                    request.getBizType(), request.getEventCode());
            return false;
        }
        SubscribeMsgTemplate template = templateOpt.get();
        Map<String, String> rendered = SubscribeMessageRenderer.render(template, request.getBizFields());
        String page = request.getPage() != null ? request.getPage() : template.getDefaultPage();
        return sender.send(request.getOpenId(), template.getTemplateId(), rendered, page);
    }
}
