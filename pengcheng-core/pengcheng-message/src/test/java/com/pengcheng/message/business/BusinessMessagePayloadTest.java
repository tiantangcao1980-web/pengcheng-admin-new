package com.pengcheng.message.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务消息载荷单测
 */
@DisplayName("BusinessMessagePayload — 6 种业务消息类型")
class BusinessMessagePayloadTest {

    @Test
    @DisplayName("枚举：fromCode 大小写不敏感 / 未知返回 null")
    void enumFromCode() {
        assertThat(BusinessMessageType.fromCode("card")).isEqualTo(BusinessMessageType.CARD);
        assertThat(BusinessMessageType.fromCode("LOCATION")).isEqualTo(BusinessMessageType.LOCATION);
        assertThat(BusinessMessageType.fromCode("nope")).isNull();
        assertThat(BusinessMessageType.fromCode(null)).isNull();
    }

    @Test
    @DisplayName("CARD 客户名片 → JSON 包含 customerId/customerName/phoneMasked")
    void cardPayload() {
        BusinessMessagePayload p = BusinessMessagePayload.card(1001L, "张三", "139****1234");
        String json = p.toJson();

        assertThat(json).contains("\"businessType\":\"CARD\"");
        assertThat(json).contains("\"customerId\":1001");
        assertThat(json).contains("张三").contains("139****1234");
    }

    @Test
    @DisplayName("LOCATION 位置 → JSON 包含 lng/lat/address")
    void locationPayload() {
        BusinessMessagePayload p = BusinessMessagePayload.location(116.39, 39.90, "北京天安门");
        String json = p.toJson();

        assertThat(json).contains("\"businessType\":\"LOCATION\"");
        assertThat(json).contains("116.39").contains("39.9").contains("北京天安门");
    }

    @Test
    @DisplayName("GOODS 楼盘卡片 → JSON 包含 projectId/projectName/price")
    void goodsPayload() {
        BusinessMessagePayload p = BusinessMessagePayload.goods(2001L, "海景花园", 28000, "120-180");
        String json = p.toJson();

        assertThat(json).contains("\"businessType\":\"GOODS\"");
        assertThat(json).contains("\"projectId\":2001").contains("海景花园").contains("28000");
    }

    @Test
    @DisplayName("EVENT 系统事件 → JSON 包含 eventCode/actorName")
    void eventPayload() {
        BusinessMessagePayload p = BusinessMessagePayload.event(
                "MEMBER_JOIN", 99L, "李四", "李四加入群聊");
        String json = p.toJson();

        assertThat(json).contains("\"businessType\":\"EVENT\"")
                .contains("MEMBER_JOIN").contains("李四");
    }

    @Test
    @DisplayName("FORM 表单：自定义 data 字段保留全部")
    void formPayload() {
        BusinessMessagePayload p = BusinessMessagePayload.builder()
                .type(BusinessMessageType.FORM)
                .data(Map.of(
                        "formId", 5001L,
                        "title", "周末团建报名",
                        "fields", java.util.List.of(
                                Map.of("name", "name", "label", "姓名", "required", true),
                                Map.of("name", "phone", "label", "手机", "required", true))))
                .build();
        String json = p.toJson();
        assertThat(json).contains("FORM").contains("周末团建报名").contains("required");
    }

    @Test
    @DisplayName("CHOICE 快速选项")
    void choicePayload() {
        BusinessMessagePayload p = BusinessMessagePayload.builder()
                .type(BusinessMessageType.CHOICE)
                .data(Map.of(
                        "question", "今晚加班吗？",
                        "options", java.util.List.of(
                                Map.of("value", "yes", "label", "加班"),
                                Map.of("value", "no", "label", "回家"))))
                .build();
        String json = p.toJson();
        assertThat(json).contains("CHOICE").contains("今晚加班吗");
    }

    @Test
    @DisplayName("反序列化：fromJson 还原 type 与 data")
    void roundtrip() {
        BusinessMessagePayload original = BusinessMessagePayload.card(5L, "王五", "138****0001");
        String json = original.toJson();

        BusinessMessagePayload restored = BusinessMessagePayload.fromJson(json);

        assertThat(restored).isNotNull();
        assertThat(restored.getType()).isEqualTo(BusinessMessageType.CARD);
        assertThat(restored.getData()).containsEntry("customerName", "王五");
    }

    @Test
    @DisplayName("反序列化：非法 JSON 返回 null（容错）")
    void fromJsonInvalid() {
        assertThat(BusinessMessagePayload.fromJson(null)).isNull();
        assertThat(BusinessMessagePayload.fromJson("")).isNull();
        assertThat(BusinessMessagePayload.fromJson("not-a-json")).isNull();
        assertThat(BusinessMessagePayload.fromJson("{\"businessType\":\"UNKNOWN\"}")).isNull();
    }

    @Test
    @DisplayName("反序列化：缺少 data 字段时返回空 map")
    void fromJsonNoData() {
        BusinessMessagePayload p = BusinessMessagePayload.fromJson(
                "{\"businessType\":\"CARD\"}");
        assertThat(p).isNotNull();
        assertThat(p.getType()).isEqualTo(BusinessMessageType.CARD);
        assertThat(p.getData()).isEmpty();
    }
}
