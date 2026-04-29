package com.pengcheng.system.doc.onlyoffice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OnlyOfficeConfigBuilder")
class OnlyOfficeConfigBuilderTest {

    private OnlyOfficeProperties props;
    private OnlyOfficeConfigBuilder builder;

    @BeforeEach
    void setUp() {
        props = new OnlyOfficeProperties();
        props.setJwtEnabled(true);
        props.setJwtSecret("test_jwt_secret_key");
        props.setCallbackUrl("https://example.com/callback");
        props.setServerUrl("http://onlyoffice:80");
        builder = new OnlyOfficeConfigBuilder(props);
    }

    @Test
    @DisplayName("JWT HS256 签名格式：三段 base64url，header 包含 alg=HS256")
    void signJwt_hs256_threePartStructure() throws Exception {
        Map<String, Object> payload = Map.of("test", "value", "num", 42);
        String token = builder.signJwt(payload);

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        // 每段都是非空 base64url 字符
        for (String part : parts) {
            assertThat(part).matches("[A-Za-z0-9_\\-]+");
        }
        // header 部分解码应含 HS256
        String headerJson = new String(java.util.Base64.getUrlDecoder().decode(
                parts[0] + "==".substring(0, (4 - parts[0].length() % 4) % 4)));
        assertThat(headerJson).contains("HS256");
    }

    @Test
    @DisplayName("jwt-enabled=false 时 build 不输出 token 字段")
    void jwtDisabled_build_noTokenInConfig() {
        props.setJwtEnabled(false);

        Map<String, Object> config = builder.build(
                "dockey1", "http://file.url/doc.docx", "test.docx",
                "docx", 1L, "张三", "edit");

        assertThat(config).doesNotContainKey("token");
    }

    @Test
    @DisplayName("documentType 推断：docx→text / xlsx→spreadsheet / pptx→presentation / pdf→pdf")
    void inferDocumentType_correctMapping() throws Exception {
        Map<String, Object> docx = builder.build("k1", "url", "a.docx", "docx", 1L, "u", "edit");
        assertThat(docx.get("documentType")).isEqualTo("text");

        Map<String, Object> xlsx = builder.build("k2", "url", "b.xlsx", "xlsx", 1L, "u", "edit");
        assertThat(xlsx.get("documentType")).isEqualTo("spreadsheet");

        Map<String, Object> pptx = builder.build("k3", "url", "c.pptx", "pptx", 1L, "u", "edit");
        assertThat(pptx.get("documentType")).isEqualTo("presentation");

        Map<String, Object> pdf = builder.build("k4", "url", "d.pdf", "pdf", 1L, "u", "edit");
        assertThat(pdf.get("documentType")).isEqualTo("pdf");
    }

    @Test
    @DisplayName("mode='view' → editorConfig.mode = 'view'")
    void build_viewMode_editorConfigModeIsView() {
        Map<String, Object> config = builder.build(
                "dockey2", "http://file.url/doc.docx", "test.docx",
                "docx", 1L, "李四", "view");

        @SuppressWarnings("unchecked")
        Map<String, Object> editorConfig = (Map<String, Object>) config.get("editorConfig");
        assertThat(editorConfig.get("mode")).isEqualTo("view");
    }

    @Test
    @DisplayName("verifyJwt — 正确 token 通过，篡改 token 失败")
    void verifyJwt_validAndInvalid() throws Exception {
        Map<String, Object> payload = Map.of("key", "doc-1-abc123");
        String token = builder.signJwt(payload);

        assertThat(builder.verifyJwt(token)).isTrue();
        // 篡改最后一个字符
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertThat(builder.verifyJwt(tampered)).isFalse();
    }
}
