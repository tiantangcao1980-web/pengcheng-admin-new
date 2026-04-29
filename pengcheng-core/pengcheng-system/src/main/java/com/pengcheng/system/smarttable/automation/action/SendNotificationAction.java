package com.pengcheng.system.smarttable.automation.action;

import com.pengcheng.system.smarttable.automation.AutomationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 动作：发送站内通知（SEND_NOTIFICATION）。
 *
 * <p><b>跨模块解耦</b>：NotificationService 在 pengcheng-message 模块，
 * pengcheng-system 不能反向依赖以避免循环（message → system 已有）。
 * 通过 ApplicationContext 反射软依赖，缺失时降级为 WARN。
 *
 * <p>params 字段说明：
 * <pre>
 * {
 *   "userId":  123,                            // 接收人用户 ID
 *   "title":   "通知标题，支持 {{fieldKey}} 占位符",
 *   "content": "通知内容，支持 {{fieldKey}} 占位符",
 *   "bizType": "smarttable"                    // 可选，默认 smarttable
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendNotificationAction implements AutomationAction {

    private final ApplicationContext applicationContext;

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

        try {
            Class<?> notifClass = Class.forName("com.pengcheng.message.entity.Notification");
            Class<?> svcClass = Class.forName("com.pengcheng.message.service.NotificationService");
            Object svc = applicationContext.getBean(svcClass);

            // 反射调 Notification.builder().userId(...).title(...)....build()
            Object builder = notifClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            builder = builderClass.getMethod("userId", Long.class).invoke(builder, userId);
            builder = builderClass.getMethod("title", String.class).invoke(builder, title);
            builder = builderClass.getMethod("content", String.class).invoke(builder, content);
            builder = builderClass.getMethod("type", Integer.class).invoke(builder, 3);
            builder = builderClass.getMethod("bizType", String.class).invoke(builder, bizType);
            builder = builderClass.getMethod("bizId", Long.class).invoke(builder, event.getTableId());
            builder = builderClass.getMethod("readStatus", Integer.class).invoke(builder, 0);
            Object notification = builderClass.getMethod("build").invoke(builder);

            Method createMethod = svcClass.getMethod("createNotification", notifClass);
            createMethod.invoke(svc, notification);

            log.info("[Automation] SEND_NOTIFICATION 成功: userId={}, title={}", userId, title);
        } catch (Throwable e) {
            log.warn("[Automation] SEND_NOTIFICATION 降级（NotificationService 不可用）: userId={} title={} err={}",
                    userId, title, e.getMessage());
        }
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
