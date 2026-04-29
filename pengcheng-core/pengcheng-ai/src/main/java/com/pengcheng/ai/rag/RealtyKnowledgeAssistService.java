package com.pengcheng.ai.rag;

import com.pengcheng.ai.provider.LlmProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 房产销售知识库助手（V1.0 Sprint B 第 5+6 任务合并）
 *
 * 提供两类能力：
 *   1) 知识库分桶上传 / 查询：复用 KnowledgeBaseService + RealtyKnowledgeBucket
 *   2) AI 写作（话术 / SOP / 公告）：先 RAG 检索 → LlmProviderRouter 生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtyKnowledgeAssistService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final LlmProviderRouter llmRouter;

    // ==========================================================
    // 任务 6: 房产销售知识库分桶
    // ==========================================================

    /** 上传文档到指定桶 → 返回切片数量 */
    public int uploadToBucket(RealtyKnowledgeBucket bucket, MultipartFile file) {
        log.info("[RealtyKB] 上传到桶 {} 文件={}", bucket, file.getOriginalFilename());
        return knowledgeBaseService.processDocument(file, bucket.getProjectId());
    }

    /** 在指定桶检索答案 */
    public String queryBucket(RealtyKnowledgeBucket bucket, String question) {
        return knowledgeBaseService.queryKnowledge(question, bucket.getProjectId());
    }

    // ==========================================================
    // 任务 5: AI 写作（话术 / SOP / 公告 / 周报草稿等）
    // ==========================================================

    /**
     * AI 写作：先从话术库 RAG 检索相关上下文，再让 LLM 起草
     *
     * @param writingType 写作类型（如"客户异议话术"/"内部 SOP"/"客户公告"）
     * @param topic 主题
     * @param keywords 关键词（可选，用于 RAG 检索）
     * @param contextBucket RAG 检索的目标桶（null 时跳过检索，纯 LLM 生成）
     */
    public String writeArticle(String writingType, String topic, String keywords,
                               RealtyKnowledgeBucket contextBucket) {
        if (writingType == null || writingType.isBlank()) {
            throw new IllegalArgumentException("写作类型不能为空");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("主题不能为空");
        }

        // 1. RAG 上下文检索（可选）
        String ragContext = "";
        if (contextBucket != null) {
            String query = (keywords == null || keywords.isBlank()) ? topic : keywords;
            try {
                ragContext = knowledgeBaseService.queryKnowledge(query, contextBucket.getProjectId());
            } catch (Exception e) {
                log.warn("[RealtyKB] RAG 检索失败，使用纯 LLM 生成: {}", e.getMessage());
            }
        }

        String systemPrompt = """
                你是一名房产销售内容写作助手。请根据用户给出的"写作类型"和"主题"起草内容。
                若提供"参考资料"则优先基于资料事实，不要杜撰具体楼盘名/客户名/政策细节。
                输出风格：专业、克制、可直接转发给客户或同事。
                输出长度：300-600 字，分段清晰。
                """;
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("写作类型：").append(writingType).append("\n");
        userPrompt.append("主题：").append(topic).append("\n");
        if (keywords != null && !keywords.isBlank()) {
            userPrompt.append("关键词：").append(keywords).append("\n");
        }
        if (!ragContext.isBlank()) {
            userPrompt.append("\n参考资料（来自房产销售知识库）：\n").append(ragContext);
        }
        userPrompt.append("\n请生成完整内容。");

        try {
            String content = llmRouter.generate(systemPrompt, userPrompt.toString());
            log.info("[RealtyKB] AI 写作完成 type={} topic={} len={}",
                    writingType, topic, content == null ? 0 : content.length());
            return content;
        } catch (Exception e) {
            log.warn("[RealtyKB] AI 写作失败: {}", e.getMessage());
            return "[AI 写作失败] " + e.getMessage();
        }
    }
}
