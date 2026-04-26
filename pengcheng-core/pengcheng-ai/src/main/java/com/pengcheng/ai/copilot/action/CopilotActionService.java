package com.pengcheng.ai.copilot.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.ai.copilot.action.entity.AiCopilotActionLog;
import com.pengcheng.ai.copilot.action.mapper.AiCopilotActionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Copilot 对话动作服务（V4.0 MVP 闭环④）。
 *
 * <p>两阶段提交：
 * <ol>
 *     <li>{@link #propose(CopilotActionRequest)}：生成 actionId + confirmToken，状态 PENDING；</li>
 *     <li>{@link #confirm(Long, String)}：校验 token 后调用对应 {@link CopilotActionExecutor} 执行；</li>
 *     <li>{@link #cancel(Long, String)}：用户拒绝。</li>
 * </ol>
 * 前端在第一步弹出"将要 ${summary}，是否确认?"按钮，避免 LLM 误触敏感操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotActionService {

    /** confirmToken TTL（分钟），超过则视为过期，不允许 confirm */
    public static final long CONFIRM_TOKEN_TTL_MINUTES = 10L;

    private final AiCopilotActionLogMapper actionLogMapper;
    private final List<CopilotActionExecutor> executors;

    /**
     * 这两个字段非 final（避免与 Lombok @RequiredArgsConstructor 冲突），
     * 由内部初始化即可，无需注入。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 第一阶段：提议动作，落库 PENDING 并返回 confirmToken。
     */
    public CopilotActionProposal propose(CopilotActionRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.getActionCode() == null || request.getActionCode().isBlank()) {
            throw new IllegalArgumentException("actionCode required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId required");
        }
        AiCopilotActionLog log = new AiCopilotActionLog();
        log.setConversationId(request.getConversationId());
        log.setUserId(request.getUserId());
        log.setPagePath(request.getPagePath());
        log.setActionCode(request.getActionCode());
        log.setPayload(serializePayload(request.getPayload()));
        log.setConfirmToken(generateToken());
        log.setStatus(AiCopilotActionLog.Status.PENDING);
        log.setCreateTime(LocalDateTime.now());
        actionLogMapper.insert(log);
        return new CopilotActionProposal(
                log.getId(),
                log.getConfirmToken(),
                log.getStatus(),
                buildSummary(request));
    }

    /**
     * 第二阶段：用户在 Drawer 点确认 → 校验 token + TTL → 执行。
     *
     * @return 执行结果摘要（提供给前端展示）
     */
    public String confirm(Long actionId, String confirmToken) {
        AiCopilotActionLog log = actionLogMapper.selectById(actionId);
        if (log == null) {
            throw new IllegalArgumentException("action not found: " + actionId);
        }
        if (!AiCopilotActionLog.Status.PENDING.equals(log.getStatus())) {
            // 幂等：已经执行过的动作返回原结果，不重复执行
            if (AiCopilotActionLog.Status.EXECUTED.equals(log.getStatus())) {
                return log.getResultSummary();
            }
            throw new IllegalStateException("action not pending: " + log.getStatus());
        }
        if (log.getConfirmToken() == null || !log.getConfirmToken().equals(confirmToken)) {
            throw new IllegalArgumentException("confirmToken mismatch");
        }
        if (log.getCreateTime() != null
                && log.getCreateTime().plusMinutes(CONFIRM_TOKEN_TTL_MINUTES).isBefore(LocalDateTime.now())) {
            log.setStatus(AiCopilotActionLog.Status.CANCELLED);
            log.setErrorMessage("confirmToken expired");
            actionLogMapper.updateById(log);
            throw new IllegalStateException("confirmToken expired");
        }
        log.setStatus(AiCopilotActionLog.Status.CONFIRMED);
        actionLogMapper.updateById(log);

        try {
            String summary = doExecute(log);
            log.setStatus(AiCopilotActionLog.Status.EXECUTED);
            log.setExecutedAt(LocalDateTime.now());
            log.setResultSummary(summary);
            actionLogMapper.updateById(log);
            return summary;
        } catch (Exception e) {
            log.setStatus(AiCopilotActionLog.Status.FAILED);
            log.setErrorMessage(truncate(e.getMessage(), 480));
            actionLogMapper.updateById(log);
            CopilotActionService.log.warn("[copilot-action] execute failed actionId={} code={} err={}",
                    actionId, log.getActionCode(), e.getMessage());
            return null;
        }
    }

    /**
     * 用户拒绝动作（直接关闭确认弹窗）。
     */
    public void cancel(Long actionId, String confirmToken) {
        AiCopilotActionLog log = actionLogMapper.selectById(actionId);
        if (log == null) {
            return;
        }
        if (!AiCopilotActionLog.Status.PENDING.equals(log.getStatus())) {
            return;
        }
        if (log.getConfirmToken() != null && !log.getConfirmToken().equals(confirmToken)) {
            // token 不匹配的取消请求直接忽略
            return;
        }
        log.setStatus(AiCopilotActionLog.Status.CANCELLED);
        actionLogMapper.updateById(log);
    }

    private String doExecute(AiCopilotActionLog log) {
        for (CopilotActionExecutor executor : executors) {
            if (executor.supports(log.getActionCode())) {
                return executor.execute(log);
            }
        }
        // 没有 executor 注册：留 TODO，先返回 stub 摘要避免 5xx
        CopilotActionService.log.warn("[copilot-action] no executor for code={} (业务侧未实现 Bean)",
                log.getActionCode());
        return "[stub] 动作 " + log.getActionCode() + " 已记录，待业务实现 Executor";
    }

    private String generateToken() {
        byte[] buf = new byte[24];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            CopilotActionService.log.warn("[copilot-action] payload serialize failed: {}", e.getMessage());
            return payload.toString();
        }
    }

    private String buildSummary(CopilotActionRequest request) {
        Map<String, Object> p = request.getPayload();
        String code = request.getActionCode();
        return switch (code == null ? "" : code) {
            case AiCopilotActionLog.Code.FOLLOW_UP_CREATE -> "新建客户跟进" + (p != null && p.get("customerName") != null
                    ? "（客户：" + p.get("customerName") + "）" : "");
            case AiCopilotActionLog.Code.TODO_CREATE -> "创建待办" + (p != null && p.get("title") != null
                    ? "：" + p.get("title") : "");
            case AiCopilotActionLog.Code.APPROVAL_SUBMIT -> "提交审批" + (p != null && p.get("flowName") != null
                    ? "：" + p.get("flowName") : "");
            default -> "执行动作 " + code;
        };
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
