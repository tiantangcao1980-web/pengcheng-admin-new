package com.pengcheng.message.subscribe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 订阅消息模板渲染器
 *
 * <p>从 {@link SubscribeMsgTemplate#getFieldMappingJson()} 解析"业务字段 -> 模板字段"映射，
 * 然后用 {@link SubscribeMessageRequest#getBizFields()} 取值。</p>
 *
 * <p>fieldMappingJson 形如：
 * <pre>{"applicantName":"thing1","status":"phrase2","time":"date3"}</pre>
 *
 * <p>渲染后得到 {@code {"thing1":"<applicantName 值>", "phrase2":"<status 值>", ...}}，
 * 可直接传给微信订阅消息接口。</p>
 *
 * <p>未命中映射的字段以原 key 透传；映射存在但业务值缺失则填空字符串。</p>
 */
@Slf4j
public final class SubscribeMessageRenderer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SubscribeMessageRenderer() {
    }

    public static Map<String, String> render(SubscribeMsgTemplate template, Map<String, String> bizFields) {
        Map<String, String> rendered = new HashMap<>();
        Map<String, String> source = bizFields == null ? new HashMap<>() : bizFields;

        if (template == null || template.getFieldMappingJson() == null
                || template.getFieldMappingJson().isBlank()) {
            // 没有映射时，原值透传
            rendered.putAll(source);
            return rendered;
        }

        try {
            JsonNode node = MAPPER.readTree(template.getFieldMappingJson());
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String bizKey = names.next();
                String tplKey = node.get(bizKey).asText("");
                if (tplKey.isBlank()) {
                    continue;
                }
                String value = source.getOrDefault(bizKey, "");
                rendered.put(tplKey, value);
            }
        } catch (Exception ex) {
            log.warn("subscribe template field mapping parse failed: tplId={}, err={}",
                    template.getTemplateId(), ex.getMessage());
            rendered.putAll(source);
        }
        return rendered;
    }
}
