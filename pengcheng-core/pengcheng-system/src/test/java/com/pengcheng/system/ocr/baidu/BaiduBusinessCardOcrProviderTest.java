package com.pengcheng.system.ocr.baidu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 百度名片 OCR Provider 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>成功 token + 成功解析（结构化字段映射验证）</li>
 *   <li>token 过期后自动刷新（缓存命中 → 过期 → 再次请求时刷新）</li>
 *   <li>HTTP 失败时抛 {@link OcrCallException}</li>
 *   <li>Feature Flag 关闭时抛 {@link IllegalStateException}</li>
 *   <li>解析空 words_result 返回空 List</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class BaiduBusinessCardOcrProviderTest {

    @Mock
    private BaiduOcrHttpClient mockHttpClient;

    private BaiduOcrProperties properties;

    // ---------- fixture JSON ----------

    private static final String TOKEN_JSON =
            "{\"access_token\":\"test-token-abc\",\"expires_in\":2592000}";

    private static final String TOKEN_JSON_2 =
            "{\"access_token\":\"test-token-xyz\",\"expires_in\":2592000}";

    private static final String OCR_SUCCESS_JSON =
            "{\n" +
            "  \"words_result\": {\n" +
            "    \"NAME\":    [{\"words\": \"李雷\"}],\n" +
            "    \"COMPANY\": [{\"words\": \"腾讯科技（深圳）有限公司\"}],\n" +
            "    \"POSTION\": [{\"words\": \"高级工程师\"}],\n" +
            "    \"MOBILE\":  [{\"words\": \"13812345678\"}],\n" +
            "    \"TEL\":     [{\"words\": \"0755-86013388\"}],\n" +
            "    \"EMAIL\":   [{\"words\": \"leil@example.com\"}],\n" +
            "    \"ADDR\":    [{\"words\": \"广东省深圳市南山区科技园路1号\"}]\n" +
            "  }\n" +
            "}";

    private static final String OCR_EMPTY_WORDS_JSON =
            "{\"words_result\": {}}";

    private static final String OCR_ERROR_JSON =
            "{\"error_code\":110,\"error_msg\":\"Access token invalid or no longer valid\"}";

    @BeforeEach
    void setUp() {
        properties = new BaiduOcrProperties();
        properties.setApiKey("test-api-key");
        properties.setSecretKey("test-secret-key");
        properties.setTokenUrl("https://aip.baidubce.com/oauth/2.0/token");
        properties.setOcrUrl("https://aip.baidubce.com/rest/2.0/ocr/v1/business_card");
    }

    // ------------------------------------------------------------------ //
    //  场景 1：成功 token + 成功解析                                        //
    // ------------------------------------------------------------------ //

    @Test
    void recognize_success_mapsAllFields() {
        when(mockHttpClient.fetchToken(anyString(), anyString(), anyString()))
                .thenReturn(TOKEN_JSON);
        when(mockHttpClient.recognizeBusinessCard(anyString(), eq("test-token-abc"), anyString()))
                .thenReturn(OCR_SUCCESS_JSON);

        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        List<String> lines = provider.recognize(new byte[]{1, 2, 3});

        assertFalse(lines.isEmpty(), "结果不应为空");
        // 验证关键字段出现在行列表中
        assertTrue(lines.contains("李雷"), "应包含姓名");
        assertTrue(lines.stream().anyMatch(l -> l.contains("腾讯科技")), "应包含公司名");
        assertTrue(lines.contains("高级工程师"), "应包含职位");
        assertTrue(lines.contains("13812345678"), "应包含手机号");
        assertTrue(lines.contains("0755-86013388"), "应包含座机");
        assertTrue(lines.contains("leil@example.com"), "应包含邮箱");
        assertTrue(lines.stream().anyMatch(l -> l.contains("深圳")), "应包含地址");

        // token 只请求了一次
        verify(mockHttpClient, times(1)).fetchToken(anyString(), anyString(), anyString());
    }

    // ------------------------------------------------------------------ //
    //  场景 2：token 过期后自动刷新                                          //
    // ------------------------------------------------------------------ //

    @Test
    void recognize_tokenExpired_refreshes() throws Exception {
        // 第一次返回一个"已过期"的 token（过期时间 = 1 秒，安全间隔 = 3600 秒，立即过期）
        String expiredTokenJson =
                "{\"access_token\":\"expired-token\",\"expires_in\":1}";

        when(mockHttpClient.fetchToken(anyString(), anyString(), anyString()))
                .thenReturn(expiredTokenJson)
                .thenReturn(TOKEN_JSON_2);
        when(mockHttpClient.recognizeBusinessCard(anyString(), anyString(), anyString()))
                .thenReturn(OCR_SUCCESS_JSON);

        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        // 第一次调用：token 立即被视为过期（expires_in=1 减去 3600s margin → 负数）
        provider.recognize(new byte[]{1});
        // 第二次调用：token 依然过期，必须再次刷新
        provider.recognize(new byte[]{1});

        // 应该调用了 2 次 fetchToken
        verify(mockHttpClient, times(2)).fetchToken(anyString(), anyString(), anyString());
    }

    @Test
    void getOrRefreshToken_caches_whenNotExpired() {
        when(mockHttpClient.fetchToken(anyString(), anyString(), anyString()))
                .thenReturn(TOKEN_JSON);
        when(mockHttpClient.recognizeBusinessCard(anyString(), anyString(), anyString()))
                .thenReturn(OCR_SUCCESS_JSON);

        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        // 连续两次调用
        provider.recognize(new byte[]{1});
        provider.recognize(new byte[]{1});

        // 第二次命中缓存，fetchToken 只调用一次
        verify(mockHttpClient, times(1)).fetchToken(anyString(), anyString(), anyString());
    }

    // ------------------------------------------------------------------ //
    //  场景 3：HTTP 失败时抛 OcrCallException                               //
    // ------------------------------------------------------------------ //

    @Test
    void recognize_tokenFetchFails_throwsOcrCallException() {
        when(mockHttpClient.fetchToken(anyString(), anyString(), anyString()))
                .thenThrow(new OcrCallException("连接超时"));

        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        assertThrows(OcrCallException.class,
                () -> provider.recognize(new byte[]{1}),
                "token 请求失败应抛出 OcrCallException");
    }

    @Test
    void recognize_ocrHttpFails_throwsOcrCallException() {
        when(mockHttpClient.fetchToken(anyString(), anyString(), anyString()))
                .thenReturn(TOKEN_JSON);
        when(mockHttpClient.recognizeBusinessCard(anyString(), anyString(), anyString()))
                .thenThrow(new OcrCallException("HTTP 500"));

        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        assertThrows(OcrCallException.class,
                () -> provider.recognize(new byte[]{1}),
                "OCR 请求失败应抛出 OcrCallException");
    }

    @Test
    void recognize_ocrBusinessError_throwsOcrCallException() {
        when(mockHttpClient.fetchToken(anyString(), anyString(), anyString()))
                .thenReturn(TOKEN_JSON);
        when(mockHttpClient.recognizeBusinessCard(anyString(), anyString(), anyString()))
                .thenReturn(OCR_ERROR_JSON);

        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        OcrCallException ex = assertThrows(OcrCallException.class,
                () -> provider.recognize(new byte[]{1}),
                "业务错误码应抛出 OcrCallException");
        assertEquals(110, ex.getErrorCode(), "错误码应为 110");
    }

    // ------------------------------------------------------------------ //
    //  场景 4：Feature Flag 关闭时抛 IllegalStateException                  //
    // ------------------------------------------------------------------ //

    @Test
    void recognize_featureFlagDisabled_throwsIllegalStateException() {
        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> provider.recognize(new byte[]{1}),
                "Feature Flag 关闭应抛出 IllegalStateException");

        assertTrue(ex.getMessage().contains("pengcheng.feature.ocr.baidu"),
                "异常信息应提示配置项名称");

        // httpClient 不应被调用
        verifyNoInteractions(mockHttpClient);
    }

    // ------------------------------------------------------------------ //
    //  场景 5：空 words_result 返回空 List                                  //
    // ------------------------------------------------------------------ //

    @Test
    void parseOcrResponse_emptyWordsResult_returnsEmptyList() {
        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        List<String> lines = provider.parseOcrResponse(OCR_EMPTY_WORDS_JSON);

        assertTrue(lines.isEmpty(), "空 words_result 应返回空 List");
    }

    @Test
    void parseOcrResponse_nullWordsResult_returnsEmptyList() {
        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);

        List<String> lines = provider.parseOcrResponse("{\"log_id\":123456}");

        assertTrue(lines.isEmpty(), "无 words_result 字段应返回空 List");
    }

    // ------------------------------------------------------------------ //
    //  辅助：getProviderType                                               //
    // ------------------------------------------------------------------ //

    @Test
    void getProviderType_returnsBaidu() {
        BaiduBusinessCardOcrProvider provider =
                new BaiduBusinessCardOcrProvider(properties, mockHttpClient, true);
        assertEquals("baidu", provider.getProviderType());
    }
}
