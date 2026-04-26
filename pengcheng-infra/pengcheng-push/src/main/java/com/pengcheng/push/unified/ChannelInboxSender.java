package com.pengcheng.push.unified;

/**
 * Web 站内信下发器
 *
 * <p>由 {@code pengcheng-core/pengcheng-message} 中的 NotificationService
 * 适配实现；本接口仅约束行为，避免 push 模块强依赖 message。</p>
 */
public interface ChannelInboxSender {

    /**
     * 下发站内信
     *
     * @param userId  用户 ID（数字字符串）
     * @param title   标题
     * @param content 内容
     * @param bizType 业务类型（用于路由前端跳转）
     * @param bizId   业务 ID
     * @return 是否落库成功
     */
    boolean send(String userId, String title, String content, String bizType, Long bizId);
}
