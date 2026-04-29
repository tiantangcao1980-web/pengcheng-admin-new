package com.pengcheng.integration.wecom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.integration.config.*;
import com.pengcheng.integration.spi.dto.ContactSyncResult;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WecomContactServiceImpl 单元测试")
class WecomContactServiceImplTest {

    @Mock IntegrationProviderConfigMapper configMapper;
    @Mock IntegrationDeptMappingMapper    deptMappingMapper;
    @Mock IntegrationUserMappingMapper    userMappingMapper;
    @Mock IntegrationSyncLogMapper        syncLogMapper;
    @Mock SysUserMapper                   sysUserMapper;
    @Mock WecomTokenCache                 tokenCache;
    @Mock WecomHttpClient                 httpClient;

    // 手动构造，ObjectMapper 使用真实实例
    private WecomContactServiceImpl contactService;

    private IntegrationProviderConfig mockConfig;

    @BeforeEach
    void setUp() {
        contactService = new WecomContactServiceImpl(
                configMapper, deptMappingMapper, userMappingMapper,
                syncLogMapper, sysUserMapper, tokenCache, httpClient,
                new ObjectMapper());

        mockConfig = new IntegrationProviderConfig();
        mockConfig.setTenantId(1L);
        mockConfig.setProvider("wecom");
        mockConfig.setCorpId("wx_corp");
        mockConfig.setSecretRef("secret");
    }

    @Test
    @DisplayName("syncContacts - 同步 1 个部门 + 1 个新用户，返回 success")
    void syncContacts_newUser_shouldReturnSuccess() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");

        when(httpClient.get(contains("department/list")))
                .thenReturn(Map.of("errcode", 0, "department",
                        List.of(Map.of("id", "1", "parentid", "0", "name", "研发部"))));

        when(httpClient.post(contains("list_id"), any()))
                .thenReturn(Map.of("errcode", 0,
                        "dept_user", List.of(Map.of("userid", "u001")),
                        "next_cursor", ""));

        when(httpClient.get(contains("user/get")))
                .thenReturn(Map.of("errcode", 0, "name", "李四",
                        "mobile", "13900000001", "email", "lisi@test.com",
                        "avatar", "https://avatar/2", "department", List.of(1)));

        when(deptMappingMapper.selectOne(any())).thenReturn(null);
        when(userMappingMapper.selectOne(any())).thenReturn(null);

        doAnswer(inv -> { ((SysUser) inv.getArgument(0)).setId(100L); return 1; })
                .when(sysUserMapper).insert(any(SysUser.class));

        ContactSyncResult result = contactService.syncContacts(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeptSynced()).isEqualTo(1);
        assertThat(result.getUserCreated()).isEqualTo(1);
        assertThat(result.getUserUpdated()).isEqualTo(0);
        verify(sysUserMapper, times(1)).insert(any());
        verify(userMappingMapper, times(1)).insert(any());
        verify(syncLogMapper, times(1)).insert(any());
    }

    @Test
    @DisplayName("syncContacts - 已存在用户 mapping 时只做更新")
    void syncContacts_existingUser_shouldUpdate() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");
        when(httpClient.get(contains("department/list")))
                .thenReturn(Map.of("errcode", 0, "department", List.of()));
        when(httpClient.post(contains("list_id"), any()))
                .thenReturn(Map.of("errcode", 0,
                        "dept_user", List.of(Map.of("userid", "u002")),
                        "next_cursor", ""));
        when(httpClient.get(contains("user/get")))
                .thenReturn(Map.of("errcode", 0, "name", "王五",
                        "mobile", "null", "email", "null", "avatar", "null",
                        "department", List.of(1)));

        IntegrationUserMapping existingMapping = new IntegrationUserMapping();
        existingMapping.setId(10L);
        existingMapping.setUserId(200L);
        when(userMappingMapper.selectOne(any())).thenReturn(existingMapping);

        SysUser existingUser = new SysUser();
        existingUser.setId(200L);
        when(sysUserMapper.selectById(200L)).thenReturn(existingUser);

        ContactSyncResult result = contactService.syncContacts(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUserUpdated()).isEqualTo(1);
        assertThat(result.getUserCreated()).isEqualTo(0);
        verify(sysUserMapper, never()).insert(any());
        verify(sysUserMapper, times(1)).updateById(any());
    }

    @Test
    @DisplayName("syncContacts - cursor 翻页拉取多批用户")
    void syncContacts_cursorPagination() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");
        when(httpClient.get(contains("department/list")))
                .thenReturn(Map.of("errcode", 0, "department", List.of()));

        when(httpClient.post(contains("list_id"), any()))
                .thenReturn(Map.of("errcode", 0,
                        "dept_user", List.of(Map.of("userid", "u001")),
                        "next_cursor", "cursor_page2"))
                .thenReturn(Map.of("errcode", 0,
                        "dept_user", List.of(Map.of("userid", "u002")),
                        "next_cursor", ""));

        when(httpClient.get(contains("user/get")))
                .thenReturn(Map.of("errcode", 0, "name", "A",
                        "mobile", "null", "email", "null", "avatar", "null",
                        "department", List.of(1)));
        when(userMappingMapper.selectOne(any())).thenReturn(null);
        doAnswer(inv -> { ((SysUser) inv.getArgument(0)).setId(1L); return 1; })
                .when(sysUserMapper).insert(any());

        ContactSyncResult result = contactService.syncContacts(1L);

        assertThat(result.getUserSynced()).isEqualTo(2);
        verify(httpClient, times(2)).post(contains("list_id"), any());
    }

    @Test
    @DisplayName("syncContacts - HTTP 异常时 success=false 且写失败日志")
    void syncContacts_httpError_shouldReturnFailed() {
        when(configMapper.selectOne(any())).thenReturn(mockConfig);
        when(tokenCache.getToken(anyString(), anyString())).thenReturn("token");
        when(httpClient.get(contains("department/list")))
                .thenThrow(new WecomApiException(40001, "invalid credential"));

        ContactSyncResult result = contactService.syncContacts(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMsg()).contains("40001");
        verify(syncLogMapper, times(1)).insert(any());
    }

    @Test
    @DisplayName("syncContacts - 无配置时 success=false")
    void syncContacts_noConfig_shouldReturnFailed() {
        when(configMapper.selectOne(any())).thenReturn(null);

        ContactSyncResult result = contactService.syncContacts(999L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isNotBlank();
    }
}
