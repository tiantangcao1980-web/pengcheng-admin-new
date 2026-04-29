package com.pengcheng.system.smarttable.automation.action;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.smarttable.automation.AutomationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 动作：调用 Webhook（CALL_WEBHOOK）
 *
 * <p>向用户配置的 URL 发送 POST 请求，携带事件上下文和自定义 body。
 *
 * <p>params 字段说明：
 * <pre>
 * {
 *   "url":         "https://example.com/hook",
 *   "headers":     {"X-Token": "abc"},   // 可选额外请求头
 *   "extraBody":   {"key": "value"},     // 可选额外 body 字段
 *   "timeoutMs":   5000                  // 超时毫秒（默认 5000）
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallWebhookAction implements AutomationAction {

    private final ObjectMapper objectMapper;

    @Override
    public String type() {
        return "CALL_WEBHOOK";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Map<String, Object> params, AutomationEvent event) throws Exception {
        String url = (String) params.get("url");
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("CALL_WEBHOOK 动作缺少 url 参数");
        }

        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("tableId", event.getTableId());
        body.put("recordId", event.getRecordId());
        body.put("triggerType", event.getTriggerType().name());
        if (event.getNewRow() != null) body.put("newRow", event.getNewRow());
        if (event.getOldRow() != null) body.put("oldRow", event.getOldRow());

        Map<String, Object> extraBody = (Map<String, Object>) params.get("extraBody");
        if (extraBody != null) {
            body.putAll(extraBody);
        }

        int timeoutMs = toInt(params.get("timeoutMs"), 5000);
        String bodyJson = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.post(url)
                .body(bodyJson, "application/json")
                .timeout(timeoutMs);

        Map<String, Object> headers = (Map<String, Object>) params.get("headers");
        if (headers != null) {
            headers.forEach((k, v) -> request.header(k, String.valueOf(v)));
        }

        try (HttpResponse response = request.execute()) {
            if (!response.isOk()) {
                throw new RuntimeException("Webhook 响应非 2xx: status=" + response.getStatus()
                        + ", body=" + response.body());
            }
            log.info("[Automation] CALL_WEBHOOK 成功: url={}, status={}", url, response.getStatus());
        }
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return defaultVal; }
    }
}
