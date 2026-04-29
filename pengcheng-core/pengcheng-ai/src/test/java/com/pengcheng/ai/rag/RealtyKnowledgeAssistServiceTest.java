package com.pengcheng.ai.rag;

import com.pengcheng.ai.provider.LlmProviderRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 房产销售知识库助手单测（覆盖任务 5 + 任务 6）
 */
@DisplayName("RealtyKnowledgeAssistService — 房产 AI 写作 + 知识库分桶")
class RealtyKnowledgeAssistServiceTest {

    private KnowledgeBaseService kb;
    private LlmProviderRouter llmRouter;
    private RealtyKnowledgeAssistService service;

    @BeforeEach
    void setUp() {
        kb = mock(KnowledgeBaseService.class);
        llmRouter = mock(LlmProviderRouter.class);
        service = new RealtyKnowledgeAssistService(kb, llmRouter);
    }

    // ========== 任务 6: 知识库分桶 ==========

    @Test
    @DisplayName("枚举：fromCode 大小写不敏感")
    void bucketFromCode() {
        assertThat(RealtyKnowledgeBucket.fromCode("project")).isEqualTo(RealtyKnowledgeBucket.PROJECT);
        assertThat(RealtyKnowledgeBucket.fromCode("SCRIPT")).isEqualTo(RealtyKnowledgeBucket.SCRIPT);
        assertThatThrownBy(() -> RealtyKnowledgeBucket.fromCode("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("上传到桶：projectId 由桶决定")
    void uploadToBucket_routesByProjectId() {
        MockMultipartFile file = new MockMultipartFile("f", "话术-异议.txt",
                "text/plain", "客户嫌贵".getBytes());
        when(kb.processDocument(any(), eq(RealtyKnowledgeBucket.SCRIPT.getProjectId())))
                .thenReturn(5);

        int chunks = service.uploadToBucket(RealtyKnowledgeBucket.SCRIPT, file);

        assertThat(chunks).isEqualTo(5);
        verify(kb).processDocument(any(), eq(RealtyKnowledgeBucket.SCRIPT.getProjectId()));
    }

    @Test
    @DisplayName("查询桶：projectId 由桶决定")
    void queryBucket_routesByProjectId() {
        when(kb.queryKnowledge(eq("学校"), eq(RealtyKnowledgeBucket.PROJECT.getProjectId())))
                .thenReturn("XX 楼盘周边有 3 所学校");

        String r = service.queryBucket(RealtyKnowledgeBucket.PROJECT, "学校");

        assertThat(r).contains("3 所学校");
    }

    // ========== 任务 5: AI 写作 ==========

    @Test
    @DisplayName("AI 写作：必填校验 - writingType")
    void write_missingType() {
        assertThatThrownBy(() -> service.writeArticle("", "主题", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("写作类型");
    }

    @Test
    @DisplayName("AI 写作：必填校验 - topic")
    void write_missingTopic() {
        assertThatThrownBy(() -> service.writeArticle("话术", "  ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("主题");
    }

    @Test
    @DisplayName("AI 写作：无 RAG 桶 → 直接调 LLM，不走 RAG")
    void write_noRagBucket() {
        when(llmRouter.generate(anyString(), anyString())).thenReturn("AI 起草内容");

        String r = service.writeArticle("销售话术", "客户嫌贵", null, null);

        assertThat(r).isEqualTo("AI 起草内容");
        verify(kb, never()).queryKnowledge(anyString(), any());
    }

    @Test
    @DisplayName("AI 写作：含 RAG 桶 → 先检索后生成；提示词包含参考资料")
    void write_withRag() {
        when(kb.queryKnowledge(anyString(), eq(RealtyKnowledgeBucket.SCRIPT.getProjectId())))
                .thenReturn("【参考】客户嫌贵的处理：先理解后引导...");
        when(llmRouter.generate(anyString(), anyString())).thenReturn("基于参考的话术");

        String r = service.writeArticle("销售话术", "客户嫌贵", "嫌贵", RealtyKnowledgeBucket.SCRIPT);

        assertThat(r).isEqualTo("基于参考的话术");

        ArgumentCaptor<String> userPromptCap = ArgumentCaptor.forClass(String.class);
        verify(llmRouter).generate(anyString(), userPromptCap.capture());
        assertThat(userPromptCap.getValue()).contains("参考资料").contains("先理解后引导");
    }

    @Test
    @DisplayName("AI 写作：RAG 失败时仍正常调 LLM 生成")
    void write_ragFailureGraceful() {
        when(kb.queryKnowledge(anyString(), any()))
                .thenThrow(new RuntimeException("向量库不可达"));
        when(llmRouter.generate(anyString(), anyString())).thenReturn("纯 LLM 输出");

        String r = service.writeArticle("公告", "国庆放假", null, RealtyKnowledgeBucket.POLICY);

        assertThat(r).isEqualTo("纯 LLM 输出");
    }

    @Test
    @DisplayName("AI 写作：LLM 失败 → 返回降级文案")
    void write_llmFailureGraceful() {
        when(llmRouter.generate(anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM 超时"));

        String r = service.writeArticle("话术", "国庆问候", null, null);

        assertThat(r).contains("AI 写作失败");
    }
}
