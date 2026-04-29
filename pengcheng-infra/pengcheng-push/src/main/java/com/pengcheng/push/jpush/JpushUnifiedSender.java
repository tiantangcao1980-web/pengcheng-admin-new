package com.pengcheng.push.jpush;

import com.pengcheng.push.unified.ChannelAppSender;
import com.pengcheng.push.unified.PushChannelLog;
import com.pengcheng.push.unified.PushChannelLogMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * 极光推送统一渠道实现（E5）
 *
 * <p>实现 {@link ChannelAppSender}，将推送请求适配为极光推送 REST API。
 * 通过 {@link JpushHttpClient} 解耦 HTTP 实现，便于测试。
 *
 * <p>字段映射：
 * <pre>
 *   userId  → audience.alias
 *   title   → notification.android.title / notification.ios.alert（前缀）
 *   content → notification.android.alert / notification.ios.alert
 *   extras  → notification.android.extras / notification.ios.extras
 * </pre>
 *
 * <p>失败时写 push_channel_log，audit_status=FAIL + reason。
 */
@Slf4j
public class JpushUnifiedSender implements ChannelAppSender {

    /** 渠道编码 */
    public static final String CHANNEL_CODE = "jpush";

    /** 极光推送 REST API 基地址 */
    private static final String JPUSH_API_URL = "https://api.jpush.cn/v3/push";

    private final String appKey;
    private final String masterSecret;
    private final PushChannelLogMapper logMapper;
    private final JpushHttpClient httpClient;

    /** 生产构造函数（使用 Hutool HTTP 客户端） */
    public JpushUnifiedSender(String appKey, String masterSecret, PushChannelLogMapper logMapper) {
        this(appKey, masterSecret, logMapper, new HutoolJpushHttpClient());
    }

    /** 测试构造函数（可注入 mock HTTP 客户端） */
    JpushUnifiedSender(String appKey, String masterSecret,
                       PushChannelLogMapper logMapper, JpushHttpClient httpClient) {
        this.appKey = appKey;
        this.masterSecret = masterSecret;
        this.logMapper = logMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean sendToUser(String userId, String title, String content, Map<String, String> extras) {
        if (!isConfigured()) {
            String reason = "JPush 配置不完整（appKey 或 masterSecret 为空）";
            log.error("[JPush] {}: userId={}", reason, userId);
            auditFail(userId, title, reason);
            return false;
        }

        try {
            boolean success = doSend(userId, title, content, extras);
            if (!success) {
                auditFail(userId, title, "JPush API 返回失败");
            }
            return success;
        } catch (JpushTokenExpiredException e) {
            String reason = "JPush token/凭证已失效: " + e.getMessage();
            log.error("[JPush] {}: userId={}", reason, userId);
            auditFail(userId, title, reason);
            return false;
        } catch (JpushNetworkException e) {
            String reason = "JPush 网络异常: " + e.getMessage();
            log.error("[JPush] {}: userId={}", reason, userId);
            auditFail(userId, title, reason);
            return false;
        } catch (Exception e) {
            String reason = "JPush 未知异常: " + e.getMessage();
            log.error("[JPush] {}: userId={}", reason, userId, e);
            auditFail(userId, title, reason);
            return false;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 实际执行推送（调用极光 REST API）
     */
    private boolean doSend(String userId, String title, String content, Map<String, String> extras) {
        String payload = buildPayload(userId, title, content, extras);
        String credentials = buildCredentials();

        String response = httpClient.post(JPUSH_API_URL, credentials, payload);

        cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(response);

        // error 字段表示认证失败或参数错误
        if (json.containsKey("error")) {
            int code = json.getJSONObject("error").getInt("code", 0);
            String message = json.getJSONObject("error").getStr("message", "unknown");
            // 3002: appKey 无效，3003: masterSecret 无效，1003: token 无效
            if (code == 3002 || code == 3003 || code == 1003) {
                throw new JpushTokenExpiredException("code=" + code + ", msg=" + message);
            }
            log.error("[JPush] API 返回错误: code={}, msg={}", code, message);
            return false;
        }

        // 正常响应包含 msg_id
        if (json.containsKey("msg_id")) {
            log.info("[JPush] 推送成功: userId={}, msgId={}", userId, json.getLong("msg_id"));
            return true;
        }

        log.warn("[JPush] 未知响应: {}", response);
        return false;
    }

    /**
     * 构建极光推送 payload（按 alias 定向推送）
     */
    private String buildPayload(String userId, String title, String content, Map<String, String> extras) {
        cn.hutool.json.JSONObject extrasJson = new cn.hutool.json.JSONObject();
        if (extras != null) {
            extras.forEach(extrasJson::set);
        }

        cn.hutool.json.JSONObject androidNotification = new cn.hutool.json.JSONObject()
                .set("title", title)
                .set("alert", content)
                .set("extras", extrasJson);

        cn.hutool.json.JSONObject iosNotification = new cn.hutool.json.JSONObject()
                .set("alert", title + ": " + content)
                .set("extras", extrasJson);

        return new cn.hutool.json.JSONObject()
                .set("platform", "all")
                .set("audience", new cn.hutool.json.JSONObject()
                        .set("alias", new String[]{userId}))
                .set("notification", new cn.hutool.json.JSONObject()
                        .set("android", androidNotification)
                        .set("ios", iosNotification))
                .toString();
    }

    /**
     * 构建 Basic Auth 凭证（Base64 编码）
     */
    private String buildCredentials() {
        String raw = appKey + ":" + masterSecret;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 配置是否完整
     */
    private boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
                && masterSecret != null && !masterSecret.isBlank();
    }

    /**
     * 写失败审计日志
     */
    private void auditFail(String target, String title, String reason) {
        try {
            PushChannelLog record = PushChannelLog.builder()
                    .channel(CHANNEL_CODE)
                    .target(target)
                    .title(title)
                    .auditStatus(PushChannelLog.STATUS_FAIL)
                    .failReason(truncate(reason, 500))
                    .createTime(LocalDateTime.now())
                    .build();
            logMapper.insert(record);
        } catch (Exception ex) {
            log.warn("[JPush] 写审计日志失败: {}", ex.getMessage());
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
