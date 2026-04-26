package com.pengcheng.ai.copilot.action;

import com.pengcheng.ai.copilot.action.entity.AiCopilotActionLog;

/**
 * Copilot 动作执行器。
 * <p>
 * 三个 MVP 动作（FOLLOW_UP_CREATE / TODO_CREATE / APPROVAL_SUBMIT）的具体执行逻辑
 * 由业务侧实现 Bean 注入。如未实现，{@link CopilotActionService} 会标记为 FAILED 并记录原因，
 * 不会抛出阻塞 LLM 流式响应的异常。
 */
public interface CopilotActionExecutor {

    /** 是否处理该动作编码 */
    boolean supports(String actionCode);

    /**
     * 执行动作。
     *
     * @param log 动作日志（含 payload/userId/pagePath 等上下文）
     * @return 结果摘要文本（用于回填 result_summary，前端展示）
     */
    String execute(AiCopilotActionLog log);
}
