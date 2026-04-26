package com.pengcheng.crm.customfield;

import com.pengcheng.crm.customfield.entity.CustomFieldDef;
import com.pengcheng.crm.customfield.service.CustomFieldValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomFieldValidatorTest {

    private CustomFieldDef def(String type, boolean required, String options, String validation) {
        return CustomFieldDef.builder()
                .label("X")
                .fieldType(type)
                .required(required ? 1 : 0)
                .optionsJson(options)
                .validationJson(validation)
                .build();
    }

    /* ---------- text ---------- */

    @Test
    void text_required_blank_fails() {
        assertNotNull(CustomFieldValidator.validate(def("text", true, null, null), null));
        assertNotNull(CustomFieldValidator.validate(def("text", true, null, null), ""));
    }

    @Test
    void text_max_length_enforced() {
        String err = CustomFieldValidator.validate(
                def("text", false, null, "{\"maxLength\":3}"), "abcd");
        assertNotNull(err);
        assertTrue(err.contains("长度"));
    }

    @Test
    void text_pattern_enforced() {
        String err = CustomFieldValidator.validate(
                def("text", false, null, "{\"pattern\":\"^\\\\d+$\"}"), "abc");
        assertNotNull(err);
        assertNull(CustomFieldValidator.validate(
                def("text", false, null, "{\"pattern\":\"^\\\\d+$\"}"), "12345"));
    }

    /* ---------- number ---------- */

    @Test
    void number_must_be_numeric() {
        assertNotNull(CustomFieldValidator.validate(def("number", false, null, null), "abc"));
    }

    @Test
    void number_min_max() {
        assertNotNull(CustomFieldValidator.validate(
                def("number", false, null, "{\"min\":10}"), 5));
        assertNotNull(CustomFieldValidator.validate(
                def("number", false, null, "{\"max\":10}"), 20));
        assertNull(CustomFieldValidator.validate(
                def("number", false, null, "{\"min\":1,\"max\":10}"), 5));
    }

    /* ---------- date ---------- */

    @Test
    void date_iso_local_date_ok() {
        assertNull(CustomFieldValidator.validate(def("date", false, null, null), "2026-04-26"));
        assertNull(CustomFieldValidator.validate(def("date", false, null, null), "2026-04-26 10:00:00"));
    }

    @Test
    void date_invalid_string_fails() {
        assertNotNull(CustomFieldValidator.validate(def("date", false, null, null), "not-a-date"));
    }

    /* ---------- select ---------- */

    @Test
    void select_must_be_in_options() {
        String opts = "[{\"value\":\"a\",\"label\":\"A\"},{\"value\":\"b\",\"label\":\"B\"}]";
        assertNull(CustomFieldValidator.validate(def("select", false, opts, null), "a"));
        assertNotNull(CustomFieldValidator.validate(def("select", false, opts, null), "z"));
    }

    /* ---------- multi_select ---------- */

    @Test
    void multi_select_requires_collection_and_each_in_options() {
        String opts = "[{\"value\":\"a\"},{\"value\":\"b\"},{\"value\":\"c\"}]";
        assertNotNull(CustomFieldValidator.validate(def("multi_select", false, opts, null), "a"));
        assertNotNull(CustomFieldValidator.validate(def("multi_select", false, opts, null), Arrays.asList("a", "x")));
        assertNull(CustomFieldValidator.validate(def("multi_select", false, opts, null), Arrays.asList("a", "b")));
    }

    /* ---------- file ---------- */

    @Test
    void file_accepts_url_and_url_array() {
        assertNull(CustomFieldValidator.validate(def("file", false, null, null), "https://x/y.png"));
        assertNull(CustomFieldValidator.validate(def("file", false, null, null),
                List.of("https://x/y.png", "minio://bk/k.jpg")));
        assertNotNull(CustomFieldValidator.validate(def("file", false, null, null), "ftp://nope"));
        assertNotNull(CustomFieldValidator.validate(def("file", false, null, null), 123));
    }

    /* ---------- 综合 ---------- */

    @Test
    void unknown_type_yields_error() {
        assertNotNull(CustomFieldValidator.validate(def("unknown_type", false, null, null), "x"));
    }

    @Test
    void supportedTypes_contains_all_six() {
        List<String> types = CustomFieldValidator.SUPPORTED_TYPES;
        assertTrue(types.contains("text"));
        assertTrue(types.contains("number"));
        assertTrue(types.contains("date"));
        assertTrue(types.contains("select"));
        assertTrue(types.contains("multi_select"));
        assertTrue(types.contains("file"));
    }
}
