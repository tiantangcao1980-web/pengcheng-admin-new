package com.pengcheng.integration.wecom;

import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import com.pengcheng.integration.spi.dto.ApprovalSyncEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WecomApprovalServiceImpl 单元测试")
class WecomApprovalServiceImplTest {

    @Mock IntegrationProviderConfigMapper configMapper;
    @Mock WecomTokenCache                 tokenCache;
    @Mock WecomHttpClient                 httpClient;

    @InjectMocks
    WecomApprovalServiceImpl approvalService;

    private IntegrationProviderConfig mockConfig;

    @BeforeEach
    void setUp() {
        mockConfig = new IntegrationProviderConfig();
        mockConfig.setTenantId(1L);
        mockConfig.setProvider("wecom");
        mockConfig.setCorpId("wx_corp");
        mockConfig.setSecretRef("secret");
    }

    @Test
    @DisplayName("pushApproval - 正常推送，请求体包含 creator_userid 和 template_id")
    void pushApproval_shouldBuildCorrectBody() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");
        when(httpClient.post(contains("applyevent"), any()))
                .thenReturn(Map.of("errcode", 0));

        ApprovalSyncEvent event = new ApprovalSyncEvent()
                .setApprovalId(1001L)
                .setSpName("请假申请")
                .setTemplateId("TMPL_001")
                .setApplicantExternalId("zhangsan")
                .setFields(List.of(
                        Map.of("ctrlId", "leaveType", "ctrlValue", "年假"),
                        Map.of("ctrlId", "days", "ctrlValue", "3")
                ));

        approvalService.pushApproval(1L, event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).post(contains("applyevent"), captor.capture());

        Map<String, Object> body = captor.getValue();
        assertThat(body.get("creator_userid")).isEqualTo("zhangsan");
        assertThat(body.get("template_id")).isEqualTo("TMPL_001");

        @SuppressWarnings("unchecked")
        Map<String, Object> applyData = (Map<String, Object>) body.get("apply_data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) applyData.get("contents");
        assertThat(contents).hasSize(2);
        assertThat(contents.get(0).get("id")).isEqualTo("leaveType");
    }

    @Test
    @DisplayName("pushApproval - API 返回错误码时抛出 WecomApiException")
    void pushApproval_apiError_shouldPropagate() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");
        when(httpClient.post(contains("applyevent"), any()))
                .thenThrow(new WecomApiException(60020, "template not found"));

        ApprovalSyncEvent event = new ApprovalSyncEvent()
                .setApprovalId(1002L)
                .setTemplateId("BAD_TMPL")
                .setApplicantExternalId("lisi");

        assertThatThrownBy(() -> approvalService.pushApproval(1L, event))
                .isInstanceOf(WecomApiException.class)
                .hasMessageContaining("60020");
    }
}
