package com.pengcheng.ai.copilot.action;

import java.util.Optional;

/**
 * Copilot 二次确认 Token 存储接口。
 *
 * <p>Token 设计原则：
 * <ul>
 *   <li>一次性消费（verifyAndConsume 成功后立即删除，防重放）</li>
 *   <li>绑定 userId（防越权：只有发起者才能消费）</li>
 *   <li>有 TTL（防长时间悬挂的 PENDING 动作被延迟确认）</li>
 * </ul>
 */
public interface ConfirmTokenStore {

    /**
     * 签发 token。
     *
     * @param proposal 动作提议（携带摘要等展示信息，存入缓存供 verify 时复查）
     * @param userId   发起用户 ID
     * @return 32 字符随机 token（URL-safe Base64 / nanoid 风格）
     */
    String issue(CopilotActionProposal proposal, Long userId);

    /**
     * 校验并一次性消费 token。
     *
     * <ul>
     *   <li>token 不存在 → empty</li>
     *   <li>userId 不匹配 → empty（防越权）</li>
     *   <li>TTL 已过期 → empty</li>
     *   <li>校验通过 → 删除缓存条目后返回对应 proposal</li>
     * </ul>
     *
     * @param token  客户端回传的 token
     * @param userId 当前请求用户 ID
     * @return 对应的 {@link CopilotActionProposal}，或 empty
     */
    Optional<CopilotActionProposal> verifyAndConsume(String token, Long userId);
}
