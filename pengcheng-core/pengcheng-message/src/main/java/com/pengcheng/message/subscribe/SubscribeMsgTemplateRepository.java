package com.pengcheng.message.subscribe;

import java.util.Optional;

/**
 * 订阅消息模板仓储接口（薄抽象）
 *
 * <p>具体实现使用 MyBatis-Plus 查询 subscribe_msg_template 表；
 * 单测以内存 stub 替换。</p>
 */
public interface SubscribeMsgTemplateRepository {

    Optional<SubscribeMsgTemplate> findEnabled(String bizType, String eventCode);
}
