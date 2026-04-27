package com.pengcheng.system.ocr.baidu;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pengcheng.system.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 百度名片 OCR Provider
 *
 * <p>调用百度 AI 开放平台 {@code /rest/2.0/ocr/v1/business_card} 接口，
 * 将结构化返回字段转为文本行交给 {@link com.pengcheng.system.ocr.BusinessCardParser} 解析。</p>
 *
 * <h3>access_token 缓存策略</h3>
 * <ul>
 *   <li>内存 Map 缓存，key = apiKey，value = {@link TokenEntry}</li>
 *   <li>有效期 = expires_in（秒）- 3600（1 小时安全间隔），默认 30 天 ≈ 2592000 秒</li>
 *   <li>使用 {@link ReentrantReadWriteLock} 保证线程安全：读时共享，刷新时独占</li>
 *   <li>双检锁避免并发重复刷新</li>
 * </ul>
 *
 * <h3>Feature Flag</h3>
 * <p>由 {@link BaiduOcrAutoConfiguration} 通过 {@code @ConditionalOnProperty} 控制，
 * 关闭时 Bean 不注入；此处额外接收 {@code enabled} 参数以支持编程式构造（测试用）。</p>
 */
@Slf4j
public class BaiduBusinessCardOcrProvider implements OcrProvider {

    private static final String PROVIDER_TYPE = "baidu";
    /** access_token 提前 1 小时过期的安全间隔（毫秒） */
    private static final long SAFETY_MARGIN_MS = 3_600_000L;

    private final BaiduOcrProperties properties;
    private final BaiduOcrHttpClient httpClient;
    private final boolean enabled;

    // ---- token 缓存（线程安全） ----
    private volatile TokenEntry cachedToken;
    private final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();

    public BaiduBusinessCardOcrProvider(BaiduOcrProperties properties,
                                        BaiduOcrHttpClient httpClient,
                                        boolean enabled) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.enabled = enabled;
    }

    // ------------------------------------------------------------------ //
    //  OcrProvider 接口                                                    //
    // ------------------------------------------------------------------ //

    @Override
    public List<String> recognize(byte[] imageBytes) {
        if (!enabled) {
            throw new IllegalStateException(
                    "百度 OCR 未启用。请在配置中设置 pengcheng.feature.ocr.baidu=true " +
                    "并配置 pengcheng.ocr.baidu.api-key / secret-key。");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return List.of();
        }

        String imageBase64 = Base64.encode(imageBytes);
        String token = getOrRefreshToken();
        String responseJson = httpClient.recognizeBusinessCard(
                properties.getOcrUrl(), token, imageBase64);

        return parseOcrResponse(responseJson);
    }

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }

    // ------------------------------------------------------------------ //
    //  access_token 缓存逻辑                                               //
    // ------------------------------------------------------------------ //

    /**
     * 读缓存有效则直接返回，否则加写锁刷新（双检锁）。
     */
    String getOrRefreshToken() {
        // 快速读路径
        tokenLock.readLock().lock();
        try {
            if (isTokenValid(cachedToken)) {
                return cachedToken.accessToken;
            }
        } finally {
            tokenLock.readLock().unlock();
        }

        // 慢写路径
        tokenLock.writeLock().lock();
        try {
            // 双检：有可能其它线程已刷新
            if (isTokenValid(cachedToken)) {
                return cachedToken.accessToken;
            }
            cachedToken = doFetchToken();
            log.info("百度 OCR access_token 已刷新，过期时间: {}",
                    new java.util.Date(cachedToken.expiresAtMs));
            return cachedToken.accessToken;
        } finally {
            tokenLock.writeLock().unlock();
        }
    }

    private boolean isTokenValid(TokenEntry entry) {
        return entry != null && System.currentTimeMillis() < entry.expiresAtMs;
    }

    private TokenEntry doFetchToken() {
        String json = httpClient.fetchToken(
                properties.getTokenUrl(),
                properties.getApiKey(),
                properties.getSecretKey());

        JSONObject obj = parseJson(json, "token 响应");

        // 百度返回 error 字段时表示失败
        if (obj.containsKey("error")) {
            throw new OcrCallException(
                    "百度 OCR token 获取失败: " + obj.getStr("error_description", obj.getStr("error")));
        }

        String accessToken = obj.getStr("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new OcrCallException("百度 OCR token 响应中 access_token 为空");
        }

        // expires_in 单位秒，默认 30 天
        long expiresIn = obj.getLong("expires_in", 2592000L);
        long expiresAtMs = System.currentTimeMillis()
                + expiresIn * 1000L
                - SAFETY_MARGIN_MS;

        return new TokenEntry(accessToken, expiresAtMs);
    }

    // ------------------------------------------------------------------ //
    //  OCR 响应解析                                                        //
    // ------------------------------------------------------------------ //

    /**
     * 将百度名片 OCR 结构化结果转为文本行列表。
     *
     * <p>百度返回格式（简化）：
     * <pre>
     * {
     *   "words_result": {
     *     "NAME":    [{"words": "李雷"}],
     *     "COMPANY": [{"words": "腾讯科技（深圳）有限公司"}],
     *     "POSTION": [{"words": "高级工程师"}],
     *     "TEL":     [{"words": "0755-86013388"}],
     *     "MOBILE":  [{"words": "13812345678"}],
     *     "EMAIL":   [{"words": "leil@example.com"}],
     *     "ADDR":    [{"words": "广东省深圳市南山区科技园路1号"}],
     *     "URL":     [{"words": "https://www.example.com"}]
     *   }
     * }
     * </pre>
     *
     * <p>输出行的顺序：NAME → COMPANY → POSTION → MOBILE → TEL → EMAIL → URL → ADDR
     * 与 {@link com.pengcheng.system.ocr.BusinessCardParser} 启发式规则对齐。</p>
     */
    List<String> parseOcrResponse(String responseJson) {
        JSONObject root = parseJson(responseJson, "OCR 响应");

        // 百度业务级错误
        if (root.containsKey("error_code")) {
            int code = root.getInt("error_code");
            String msg = root.getStr("error_msg", "unknown");
            throw new OcrCallException("百度名片 OCR 业务错误 error_code=" + code + ": " + msg, code);
        }

        JSONObject wordsResult = root.getJSONObject("words_result");
        if (wordsResult == null) {
            log.warn("百度名片 OCR 响应中 words_result 为空，返回空结果");
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        appendWordsField(lines, wordsResult, "NAME");
        appendWordsField(lines, wordsResult, "COMPANY");
        appendWordsField(lines, wordsResult, "POSTION");   // 百度拼写为 POSTION（非 POSITION）
        appendWordsField(lines, wordsResult, "MOBILE");
        appendWordsField(lines, wordsResult, "TEL");
        appendWordsField(lines, wordsResult, "EMAIL");
        appendWordsField(lines, wordsResult, "URL");
        appendWordsField(lines, wordsResult, "ADDR");
        return lines;
    }

    /**
     * 从 words_result 的某个字段中提取所有 words 值追加到 lines。
     *
     * <p>百度名片接口每个字段值为数组，例如
     * {@code "TEL": [{"words": "010-12345678"}, {"words": "0755-8888"}]}。</p>
     */
    private void appendWordsField(List<String> lines, JSONObject wordsResult, String fieldName) {
        JSONArray arr = wordsResult.getJSONArray(fieldName);
        if (arr == null || arr.isEmpty()) {
            return;
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            if (item != null) {
                String words = item.getStr("words");
                if (words != null && !words.isBlank()) {
                    lines.add(words.trim());
                }
            }
        }
    }

    private JSONObject parseJson(String json, String context) {
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            throw new OcrCallException("解析百度 OCR " + context + " JSON 失败: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  内部 DTO                                                            //
    // ------------------------------------------------------------------ //

    static final class TokenEntry {
        final String accessToken;
        /** System.currentTimeMillis() 单位，已减去安全间隔 */
        final long expiresAtMs;

        TokenEntry(String accessToken, long expiresAtMs) {
            this.accessToken = accessToken;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
