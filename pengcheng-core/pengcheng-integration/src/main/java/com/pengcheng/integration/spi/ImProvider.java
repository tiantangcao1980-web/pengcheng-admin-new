package com.pengcheng.integration.spi;

/**
 * IM 集成 Provider 标识接口。
 * <p>
 * 每个 Provider 实现均需声明唯一 {@code provider()} 标识（wecom / dingtalk / feishu），
 * 以便工厂/注册中心按标识路由。
 */
public interface ImProvider {

    /**
     * Provider 唯一标识（小写），取值：wecom / dingtalk / feishu
     */
    String provider();

    /**
     * Provider 中文显示名称
     */
    String providerName();
}
