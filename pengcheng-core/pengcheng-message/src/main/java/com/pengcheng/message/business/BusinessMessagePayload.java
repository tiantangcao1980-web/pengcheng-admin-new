package com.pengcheng.message.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务消息载荷
 *
 * 序列化后落入 sys_chat_message.extra JSON 字段；
 * 前端按 type 分发到对应渲染组件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessMessagePayload {

    /** 业务消息类型 */
    private BusinessMessageType type;

    /**
     * 业务字段（不同 type 的字段不同）
     * CARD     : { customerId, customerName, phoneMasked, dealProbability }
     * LOCATION : { lng, lat, address, customerId? }
     * GOODS    : { projectId, projectName, price, area, image }
     * FORM     : { formId, title, fields:[{name,label,type,required}] }
     * CHOICE   : { question, options:[{value,label}] }
     * EVENT    : { eventCode, actorId, actorName, message }
     */
    private Map<String, Object> data;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 序列化为 JSON 字符串（写入 extra 字段） */
    public String toJson() {
        try {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("businessType", type.name());
            wrapper.put("data", data == null ? Map.of() : data);
            return MAPPER.writeValueAsString(wrapper);
        } catch (Exception e) {
            throw new RuntimeException("BusinessMessagePayload 序列化失败", e);
        }
    }

    /** 从 JSON 反序列化 */
    public static BusinessMessagePayload fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = MAPPER.readValue(json, Map.class);
            BusinessMessageType type = BusinessMessageType.fromCode(
                    (String) map.get("businessType"));
            if (type == null) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) map.getOrDefault("data", Map.of());
            return BusinessMessagePayload.builder().type(type).data(data).build();
        } catch (Exception e) {
            return null;
        }
    }

    // ============== 便捷构造 ==============

    public static BusinessMessagePayload card(Long customerId, String customerName,
                                              String phoneMasked) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customerId", customerId);
        data.put("customerName", customerName);
        data.put("phoneMasked", phoneMasked);
        return BusinessMessagePayload.builder().type(BusinessMessageType.CARD).data(data).build();
    }

    public static BusinessMessagePayload location(Double lng, Double lat, String address) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lng", lng);
        data.put("lat", lat);
        data.put("address", address);
        return BusinessMessagePayload.builder().type(BusinessMessageType.LOCATION).data(data).build();
    }

    public static BusinessMessagePayload goods(Long projectId, String projectName,
                                               Object price, Object area) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("projectId", projectId);
        data.put("projectName", projectName);
        data.put("price", price);
        data.put("area", area);
        return BusinessMessagePayload.builder().type(BusinessMessageType.GOODS).data(data).build();
    }

    public static BusinessMessagePayload event(String eventCode, Long actorId,
                                               String actorName, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eventCode", eventCode);
        data.put("actorId", actorId);
        data.put("actorName", actorName);
        data.put("message", message);
        return BusinessMessagePayload.builder().type(BusinessMessageType.EVENT).data(data).build();
    }
}
