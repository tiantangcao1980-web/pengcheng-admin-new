package com.pengcheng.hr.okr.ai;

import com.pengcheng.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OKR AI 辅助拆解服务
 * <p>
 * 根据目标标题和描述，调用 AiChatService 生成 3-5 个 Key Result 建议。
 * 若 AI 调用失败，fallback 返回空列表 + WARN 日志，不阻断业务。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OkrSuggestionService {

    private static final String PROMPT_TEMPLATE =
            "你是 OKR 目标管理专家。请根据以下目标，建议 3-5 个可衡量的关键结果（Key Results）。\n"
            + "要求：每个 KR 必须可衡量（含数字/比例/里程碑），一行一个，仅输出 KR 文本，不要编号或额外解释。\n\n"
            + "目标标题：%s\n"
            + "目标描述：%s";

    private final AiChatService aiChatService;

    /**
     * 根据目标标题和描述，返回 AI 建议的 KR 列表（3-5 条）。
     * 调用失败时返回空列表，业务层应将此视为「无建议」而非异常。
     *
     * @param objectiveTitle       目标标题
     * @param objectiveDescription 目标描述（可为 null）
     * @return KR 建议文本列表
     */
    public List<String> suggestKeyResults(String objectiveTitle, String objectiveDescription) {
        if (!StringUtils.hasText(objectiveTitle)) {
            return Collections.emptyList();
        }
        String desc = StringUtils.hasText(objectiveDescription) ? objectiveDescription : "（无描述）";
        String prompt = String.format(PROMPT_TEMPLATE, objectiveTitle, desc);
        try {
            // TODO: 替换为更轻量的 AI 调用（如专用的 KR 生成 prompt pipeline）
            AiChatService.ChatResult result = aiChatService.chat(prompt);
            if (result == null || !StringUtils.hasText(result.content())) {
                return Collections.emptyList();
            }
            return Arrays.stream(result.content().split("\n"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[OkrSuggestion] AI 建议 KR 失败，objective={}, reason={}", objectiveTitle, e.getMessage());
            return Collections.emptyList();
        }
    }
}
