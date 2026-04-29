package com.pengcheng.hr.okr.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OKR AI 辅助拆解服务。
 *
 * <p>根据目标标题和描述，调用 AiChatService（pengcheng-ai 模块）生成 3-5 个 Key Result 建议。
 *
 * <p><b>跨模块解耦</b>：pengcheng-hr 不能直接依赖 pengcheng-ai（ai → realty → hr 会循环），
 * 通过 ApplicationContext 反射软依赖。AI 模块缺失时 fallback 返回空列表 + WARN 日志，不阻断业务。
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

    private final ApplicationContext applicationContext;

    /**
     * 根据目标标题和描述，返回 AI 建议的 KR 列表（3-5 条）。
     * 调用失败时返回空列表，业务层应将此视为「无建议」而非异常。
     */
    public List<String> suggestKeyResults(String objectiveTitle, String objectiveDescription) {
        if (!StringUtils.hasText(objectiveTitle)) {
            return Collections.emptyList();
        }
        String desc = StringUtils.hasText(objectiveDescription) ? objectiveDescription : "（无描述）";
        String prompt = String.format(PROMPT_TEMPLATE, objectiveTitle, desc);

        try {
            // 反射软依赖 AiChatService（pengcheng-ai 模块；不在 classpath 时降级）
            Class<?> svcClass = Class.forName("com.pengcheng.ai.service.AiChatService");
            Class<?> resultClass = Class.forName("com.pengcheng.ai.service.AiChatService$ChatResult");
            Object svc = applicationContext.getBean(svcClass);
            Object result = svcClass.getMethod("chat", String.class).invoke(svc, prompt);
            if (result == null) return Collections.emptyList();
            String content = (String) resultClass.getMethod("content").invoke(result);
            if (!StringUtils.hasText(content)) return Collections.emptyList();
            return Arrays.stream(content.split("\n"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            log.warn("[OkrSuggestion] AI 建议 KR 失败，objective={}, reason={}",
                    objectiveTitle, e.getMessage());
            return Collections.emptyList();
        }
    }
}
