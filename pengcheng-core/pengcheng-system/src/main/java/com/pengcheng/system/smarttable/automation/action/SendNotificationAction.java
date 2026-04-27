package com.pengcheng.system.smarttable.automation.action;

import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.system.smarttable.automation.AutomationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 动作：发送站内通知（SEND_NOTIFICATION）
 *
 * <p>params 字段说明：
 * <pre>
 * {
 *   "userId":  123,                          // 接收人用户 ID
 *   "title":   "通知标题，支持 {{fieldKey}} 占位符",
 *   "content": "通知内容，支持 {{fieldKey}} 占位符",
 *   "bizType": "smarttable"                  // 可选，默认 smarttable
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendNotificationAction implements AutomationAction {

    private final NotificationService notificationService;

    @Override
    public String type() {
        return "SEND_NOTIFICATION";
    }

    @Override
    public void execute(Map<String, Object> params, AutomationEvent event) {
        Long userId = toLong(params.get("userId"));
        if (userId == null) {
            throw new IllegalArgumentException("SEND_NOTIFICATION 动作缺少 userId 参数");
        }
        String title = renderTemplate((String) params.getOrDefault("title", "多维表格自动化通知"), event);
        String content = renderTemplate((String) params.getOrDefault("content", ""), event);
        String bizType = (String) params.getOrDefault("bizType", "smarttable");

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type(3)
                .bizType(bizType)
                .bizId(event.getTableId())
                .readStatus(0)
                .build();

        notificationService.createNotification(notification);
        log.info("[Automation] SEND_NOTIFICATION 成功: userId={}, title={}", userId, title);
    }

    private String renderTemplate(String template, AutomationEvent event) {
        if (template == null) return "";
        Map<String, Object> row = event.getNewRow() != null ? event.getNewRow() : event.getOldRow();
        if (row == null) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return result;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }
}
