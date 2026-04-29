package com.pengcheng.ai.orchestration.tool;

import com.pengcheng.ai.orchestration.AgentIntent;
import com.pengcheng.ai.orchestration.AiToolContext;
import com.pengcheng.ai.orchestration.OrchestratedChatResult;
import com.pengcheng.ai.tools.RealtyDataTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 房产领域智能体工具
 *
 * 处理 REALTY 意图的房产数据查询，封装 RealtyDataTools 现有 @Tool 方法。
 * 关键词命中：楼盘 / 户型 / 开盘 / 认筹 / 签约 / 回款 / 佣金 / 商机
 *
 * 对接：
 *   - 成交查询 → RealtyDataTools.getDealSummary
 *   - 回款总览 → RealtyDataTools.getReceivableOverview
 *
 * 多轮：当前为单轮 fan-out（按消息关键词调用对应底层方法），
 * 高级用法可改造为 LLM Tool Calling 让 Spring AI 自动选择。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtyAgentTool implements AiAgentTool {

    private final RealtyDataTools realtyDataTools;

    @Override
    public AgentIntent supportedIntent() {
        return AgentIntent.REALTY;
    }

    @Override
    public String toolName() {
        return "realty-agent";
    }

    @Override
    public OrchestratedChatResult execute(AiToolContext context) {
        String message = context.message();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "realty_summary");
        payload.put("agent", toolName());

        String content;
        try {
            if (mentionsReceivable(message)) {
                Object overview = realtyDataTools.getReceivableOverview();
                payload.put("data", overview);
                content = formatReceivable(overview);
            } else {
                // 默认回答成交概览
                Map<String, Object> deal = realtyDataTools.getDealSummary(null, null);
                payload.put("data", deal);
                content = formatDeal(deal);
            }
        } catch (Exception e) {
            log.warn("[RealtyAgentTool] 查询失败: {}", e.getMessage());
            content = "查询房产数据时出现异常：" + e.getMessage();
            payload.put("error", e.getMessage());
        }

        return new OrchestratedChatResult(
                content,
                "text",
                context.conversationId(),
                toolName(),
                payload
        );
    }

    private boolean mentionsReceivable(String message) {
        if (!StringUtils.hasText(message)) return false;
        String lower = message.toLowerCase();
        return lower.contains("回款") || lower.contains("应收")
                || lower.contains("逾期") || lower.contains("现金流");
    }

    private String formatDeal(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return "今日无成交数据。";
        StringBuilder sb = new StringBuilder("【房产成交速览】\n");
        data.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }

    private String formatReceivable(Object data) {
        if (data == null) return "暂无回款数据。";
        return "【回款总览】\n" + data.toString();
    }
}
