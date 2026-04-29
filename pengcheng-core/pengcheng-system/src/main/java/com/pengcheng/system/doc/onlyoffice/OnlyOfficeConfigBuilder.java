package com.pengcheng.system.doc.onlyoffice;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 构建 OnlyOffice 编辑器配置 + JWT 签名（按 OnlyOffice 文档：
 * <a href="https://api.onlyoffice.com/editors/signature/">JWT 签名规范</a>）。
 *
 * <p>JWT 算法：HS256（与 OnlyOffice Server 默认一致）。
 *
 * <h3>使用</h3>
 * 前端调 {@code GET /admin/onlyoffice/config?docId=...} 拿到完整 config 对象，
 * 直接传给 {@code DocsAPI.DocEditor(elementId, config)} 即可。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pengcheng.feature.onlyoffice", havingValue = "true")
public class OnlyOfficeConfigBuilder {

    private final OnlyOfficeProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成 OnlyOffice editor config（含 JWT token）。
     *
     * @param docKey  会话唯一标识（同一个文档同一份内容生成同 key；内容变了 key 必须变）
     * @param fileUrl OnlyOffice 服务端可访问的源文件 URL（建议带签名 token）
     * @param fileName 文件名（用于扩展名识别）
     * @param fileType 扩展名（不带点：docx/xlsx/pptx/txt/md）
     * @param userId 当前用户 ID（OnlyOffice 用于多人协作染色）
     * @param userName 当前用户姓名
     * @param mode editor mode：edit / view
     */
    public Map<String, Object> build(String docKey, String fileUrl, String fileName,
                                      String fileType, Long userId, String userName, String mode) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", fileType);
        document.put("key", docKey);
        document.put("title", fileName);
        document.put("url", fileUrl);

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("mode", "edit".equalsIgnoreCase(mode) ? "edit" : "view");
        editorConfig.put("lang", "zh-CN");
        editorConfig.put("callbackUrl", props.getCallbackUrl());
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", String.valueOf(userId));
        user.put("name", userName != null ? userName : "User-" + userId);
        editorConfig.put("user", user);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("document", document);
        config.put("editorConfig", editorConfig);
        config.put("documentType", inferDocumentType(fileType));
        config.put("type", "desktop");

        if (props.isJwtEnabled() && props.getJwtSecret() != null && !props.getJwtSecret().isBlank()) {
            try {
                String token = signJwt(config);
                config.put("token", token);
            } catch (Exception e) {
                log.error("[OnlyOffice] JWT 签名失败", e);
                throw new IllegalStateException("OnlyOffice JWT 签名失败", e);
            }
        }
        return config;
    }

    /** OnlyOffice DocumentType：text/spreadsheet/presentation/pdf */
    private String inferDocumentType(String fileType) {
        if (fileType == null) return "text";
        switch (fileType.toLowerCase()) {
            case "xlsx":
            case "xls":
            case "csv":
                return "spreadsheet";
            case "pptx":
            case "ppt":
                return "presentation";
            case "pdf":
                return "pdf";
            default:
                return "text";
        }
    }

    /** HS256 JWT：header.payload.signature */
    String signJwt(Map<String, Object> payload) throws Exception {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        String headerB64 = base64Url(objectMapper.writeValueAsString(header));
        String payloadB64 = base64Url(objectMapper.writeValueAsString(payload));
        String signingInput = headerB64 + "." + payloadB64;

        HMac mac = new HMac(HmacAlgorithm.HmacSHA256, props.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = mac.digest(signingInput);
        String sigB64 = base64UrlBytes(sigBytes);
        return signingInput + "." + sigB64;
    }

    /** 校验来自 OnlyOffice 回调的 JWT token。 */
    public boolean verifyJwt(String token) {
        if (!props.isJwtEnabled()) return true; // 关闭时不校验
        if (token == null) return false;
        try {
            int dot1 = token.indexOf('.');
            int dot2 = token.indexOf('.', dot1 + 1);
            if (dot1 < 0 || dot2 < 0) return false;
            String signingInput = token.substring(0, dot2);
            String givenSig = token.substring(dot2 + 1);
            HMac mac = new HMac(HmacAlgorithm.HmacSHA256, props.getJwtSecret().getBytes(StandardCharsets.UTF_8));
            String expected = base64UrlBytes(mac.digest(signingInput));
            return expected.equals(givenSig);
        } catch (Exception e) {
            log.warn("[OnlyOffice] JWT 校验异常: {}", e.getMessage());
            return false;
        }
    }

    private String base64Url(String text) {
        return base64UrlBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlBytes(byte[] bytes) {
        return Base64.encodeUrlSafe(bytes).replaceAll("=+$", "");
    }
}
