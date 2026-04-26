package com.pengcheng.message.channel;

/**
 * 用户三通道画像解析器
 *
 * <p>由具体业务（如 system 模块）实现，从 sys_user / sys_user_device /
 * mp_user_subscribe 等表组合出最终画像。</p>
 *
 * <p>本接口故意保持极简，便于单测注入 stub。</p>
 */
public interface UserChannelResolver {

    /**
     * 加载用户画像；不存在时返回的 {@link UserChannelProfile} 至少包含 userId，
     * 让调度器走站内信兜底。
     */
    UserChannelProfile resolve(Long userId);
}
