package com.pengcheng.integration.wecom;

import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import com.pengcheng.integration.config.IntegrationUserMapping;
import com.pengcheng.integration.config.IntegrationUserMappingMapper;
import com.pengcheng.integration.spi.dto.ImCardMessage;
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
@DisplayName("WecomMessageServiceImpl 单元测试")
class WecomMessageServiceImplTest {

    @Mock IntegrationProviderConfigMapper configMapper;
    @Mock IntegrationUserMappingMapper    userMappingMapper;
    @Mock WecomTokenCache                 tokenCache;
    @Mock WecomHttpClient                 httpClient;

    @InjectMocks
    WecomMessageServiceImpl messageService;

    private IntegrationProviderConfig mockConfig;

    @BeforeEach
    void setUp() {
        mockConfig = new IntegrationProviderConfig();
        mockConfig.setTenantId(1L);
        mockConfig.setProvider("wecom");
        mockConfig.setCorpId("wx_corp");
        mockConfig.setAgentId("1000001");
        mockConfig.setSecretRef("secret");
    }

    @Test
    @DisplayName("sendText - 正常发送，touser 按 | 拼接 externalId")
    void sendText_shouldJoinExternalIdsWithPipe() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");

        IntegrationUserMapping m1 = new IntegrationUserMapping();
        m1.setUserId(1L);
        m1.setExternalId("wangwu");
        IntegrationUserMapping m2 = new IntegrationUserMapping();
        m2.setUserId(2L);
        m2.setExternalId("zhaoliu");
        when(userMappingMapper.selectList(any())).thenReturn(List.of(m1, m2));
        when(httpClient.post(contains("message/send"), any()))
                .thenReturn(Map.of("errcode", 0));

        messageService.sendText(1L, List.of(1L, 2L), "你好世界");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).post(anyString(), bodyCaptor.capture());

        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body.get("touser").toString()).contains("wangwu").contains("zhaoliu");
        assertThat(body.get("msgtype")).isEqualTo("text");
    }

    @Test
    @DisplayName("sendCard - 卡片消息 msgtype=textcard 且字段完整")
    void sendCard_shouldUseMsgTypeTextCard() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");
        when(userMappingMapper.selectList(any())).thenReturn(List.of());
        when(httpClient.post(contains("message/send"), any()))
                .thenReturn(Map.of("errcode", 0));

        ImCardMessage card = new ImCardMessage()
                .setTitle("审批提醒")
                .setDescription("您有一条待办审批，请及时处理")
                .setUrl("https://oa.example.com/approval/1")
                .setBtnTxt("去处理");

        messageService.sendCard(1L, List.of(), card);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).post(anyString(), captor.capture());

        Map<String, Object> body = captor.getValue();
        assertThat(body.get("msgtype")).isEqualTo("textcard");
        @SuppressWarnings("unchecked")
        Map<String, Object> textCard = (Map<String, Object>) body.get("textcard");
        assertThat(textCard.get("title")).isEqualTo("审批提醒");
        assertThat(textCard.get("btntxt")).isEqualTo("去处理");
    }

    @Test
    @DisplayName("sendText - 无 mapping 时 touser 为空字符串，仍正常调用接口")
    void sendText_noMapping_toUserEmpty() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");
        when(userMappingMapper.selectList(any())).thenReturn(List.of());
        when(httpClient.post(contains("message/send"), any()))
                .thenReturn(Map.of("errcode", 0));

        assertThatCode(() -> messageService.sendText(1L, List.of(99L), "test"))
                .doesNotThrowAnyException();
    }
}
