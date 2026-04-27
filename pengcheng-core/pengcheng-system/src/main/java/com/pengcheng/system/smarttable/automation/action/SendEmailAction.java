package com.pengcheng.system.smarttable.automation.action;

import com.pengcheng.mail.EmailService;
import com.pengcheng.system.smarttable.automation.AutomationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 动作：发送邮件（SEND_EMAIL）
 *
 * <p>params 字段说明：
 * <pre>
 * {
 *   "to":      "recipient@example.com",
 *   "subject": "邮件主题，支持 {{fieldKey}} 占位符",
 *   "body":    "邮件正文，支持 {{fieldKey}} 占位符",
 *   "html":    false   // 是否发送 HTML 邮件（可选，默认 false）
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendEmailAction implements AutomationAction {

    private final EmailService emailService;

    @Override
    public String type() {
        return "SEND_EMAIL";
    }

    @Override
    public void execute(Map<String, Object> params, AutomationEvent event) throws Exception {
        String to = (String) params.get("to");
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("SEND_EMAIL 动作缺少 to 参数");
        }
        String subject = renderTemplate((String) params.getOrDefault("subject", "自动化通知"), event);
        String body = renderTemplate((String) params.getOrDefault("body", ""), event);
        boolean html = Boolean.TRUE.equals(params.get("html"));

        if (html) {
            emailService.sendHtmlMail(to, subject, body);
        } else {
            emailService.sendSimpleMail(to, subject, body);
        }
        log.info("[Automation] SEND_EMAIL 成功: to={}, subject={}", to, subject);
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
}
