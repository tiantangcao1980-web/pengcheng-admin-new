package com.pengcheng.ai.copilot.action;

import lombok.Data;

import java.util.Map;

/**
 * Copilot Tool Call 提议请求（前端 Drawer 弹窗 → 后端 propose）。
 */
@Data
public class CopilotActionRequest {

    /** 动作编码：FOLLOW_UP_CREATE / TODO_CREATE / APPROVAL_SUBMIT */
    private String actionCode;

    /** 关联会话 */
    private String conversationId;

    /** 触发用户 ID（前端注入或后端从上下文取） */
    private Long userId;

    /** 当前页面 path（用于审计） */
    private String pagePath;

    /** 动作参数（业务字段） */
    private Map<String, Object> payload;
}
