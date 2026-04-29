package com.pengcheng.integration.spi;

import com.pengcheng.integration.spi.dto.ImCardMessage;

import java.util.List;

/**
 * IM 消息发送服务 SPI。
 * <p>
 * 支持文本消息与卡片消息，内部自动将系统 userId 转换为外部 externalId。
 */
public interface ImMessageService {

    /**
     * 向指定用户列表发送文本消息。
     *
     * @param tenantId 租户 ID
     * @param userIds  内部系统用户 ID 列表
     * @param content  消息正文
     */
    void sendText(Long tenantId, List<Long> userIds, String content);

    /**
     * 向指定用户列表发送卡片消息（textcard）。
     *
     * @param tenantId 租户 ID
     * @param userIds  内部系统用户 ID 列表
     * @param card     卡片消息内容
     */
    void sendCard(Long tenantId, List<Long> userIds, ImCardMessage card);
}
