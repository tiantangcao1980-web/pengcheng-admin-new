package com.pengcheng.system.doc.onlyoffice;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OnlyOffice 配置（M1 — V1.0 收口）。
 *
 * <p>典型 application.yml 片段：
 * <pre>{@code
 * pengcheng:
 *   feature:
 *     onlyoffice: true
 *   onlyoffice:
 *     server-url: http://onlyoffice:80           # OnlyOffice DocumentServer 地址
 *     callback-url: https://your.host/api/onlyoffice/callback
 *     jwt-enabled: true
 *     jwt-secret: ${ONLYOFFICE_JWT_SECRET}        # 与 OnlyOffice Server 相同的 JWT 密钥
 *     jwt-header: Authorization                    # 默认值
 *     editor-lifetime-min: 30                     # 编辑会话有效期
 * }</pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "pengcheng.onlyoffice")
public class OnlyOfficeProperties {

    /** OnlyOffice DocumentServer 地址，例如 http://docs.example.com */
    private String serverUrl;

    /** 回调 URL（OnlyOffice 服务端在保存时回调本应用） */
    private String callbackUrl;

    /** 是否启用 JWT 签名（生产强烈建议 true） */
    private boolean jwtEnabled = true;

    /** JWT 密钥 — 与 OnlyOffice Server 配置一致 */
    private String jwtSecret;

    /** JWT 请求头名（OnlyOffice 默认 Authorization，前缀 Bearer） */
    private String jwtHeader = "Authorization";

    /** 编辑器会话有效期（分钟） */
    private int editorLifetimeMin = 30;
}
