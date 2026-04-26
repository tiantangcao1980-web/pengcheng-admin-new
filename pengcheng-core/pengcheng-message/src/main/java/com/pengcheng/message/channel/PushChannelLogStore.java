package com.pengcheng.message.channel;

/**
 * 推送通道日志落库接口（薄抽象，便于单测）
 */
public interface PushChannelLogStore {

    /**
     * 异步/同步落库（视实现而定）
     */
    void save(PushChannelLog log);
}
