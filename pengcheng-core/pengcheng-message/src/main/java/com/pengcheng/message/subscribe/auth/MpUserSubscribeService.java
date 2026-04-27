package com.pengcheng.message.subscribe.auth;

import java.util.Optional;

/**
 * 小程序订阅消息授权管理服务
 *
 * <p>职责：
 * <ul>
 *   <li>记录用户通过 {@code wx.requestSubscribeMessage} 授权的订阅配额</li>
 *   <li>推送前原子消费一次配额</li>
 *   <li>支持用户撤销授权</li>
 * </ul>
 */
public interface MpUserSubscribeService {

    /**
     * 记录用户订阅（客户端 wx.requestSubscribeMessage 成功回调后调用）。
     *
     * <p>若该 (userId, templateId) 行不存在则插入；
     * 若已存在则将 quota += quotaIncrement，并更新 openId 和 lastSubscribeTime，
     * 同时重置 revoked = 0（用户重新授权视为解除撤销）。</p>
     *
     * @param userId         系统用户 ID
     * @param openId         微信小程序 openId
     * @param templateId     订阅消息模板 ID
     * @param quotaIncrement 本次授权新增的配额（通常为 1）
     */
    void recordSubscribe(Long userId, String openId, String templateId, int quotaIncrement);

    /**
     * 尝试消费一次配额（推送前调用）。
     *
     * <p>通过原子 UPDATE 实现：{@code used + 1 WHERE used < quota AND revoked = 0}。
     * 返回 true 表示成功扣减，可以发送；false 表示配额已耗尽或已撤销。</p>
     *
     * @param userId     系统用户 ID
     * @param templateId 订阅消息模板 ID
     * @return 是否成功消费
     */
    boolean tryConsume(Long userId, String templateId);

    /**
     * 撤销订阅（用户在小程序设置中关闭后回调）。
     *
     * <p>将 revoked 置为 1；若记录不存在则忽略。</p>
     *
     * @param userId     系统用户 ID
     * @param templateId 订阅消息模板 ID
     */
    void revoke(Long userId, String templateId);

    /**
     * 查询指定用户对指定模板的有效订阅记录（未撤销）。
     *
     * @param userId     系统用户 ID
     * @param templateId 订阅消息模板 ID
     * @return 订阅记录（若不存在或已撤销则返回 empty）
     */
    Optional<MpUserSubscribe> findActive(Long userId, String templateId);

    /**
     * 查询用户所有模板中最新的有效订阅记录（用于 Resolver 快速判断是否有可用订阅）。
     *
     * <p>返回条件：{@code revoked = 0 AND used < quota}，按 lastSubscribeTime 降序取第一条。</p>
     *
     * @param userId 系统用户 ID
     * @return 配额剩余最新的订阅记录（不存在则 empty）
     */
    Optional<MpUserSubscribe> findLatestActive(Long userId);
}
