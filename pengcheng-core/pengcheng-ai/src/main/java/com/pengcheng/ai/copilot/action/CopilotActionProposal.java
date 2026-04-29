package com.pengcheng.ai.copilot.action;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copilot 动作提议返回。
 * <p>
 * 前端拿到 confirmToken 后，在 Drawer 上弹出"是否确认执行 X"二次确认按钮，
 * 用户点确认时将 confirmToken 回传给 /confirm 端点完成执行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotActionProposal {

    /** ai_copilot_action_log 主键 */
    private Long actionId;

    /** 二次确认 token（一次性、随机、有 TTL） */
    private String confirmToken;

    /** 当前状态（PENDING） */
    private String status;

    /** 摘要文本（前端展示给用户的"将要执行什么"） */
    private String summary;
}
