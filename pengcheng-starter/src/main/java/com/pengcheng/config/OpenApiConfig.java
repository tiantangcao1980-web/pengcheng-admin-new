package com.pengcheng.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * springdoc + knife4j OpenAPI 3.0 元数据（M2 — V1.0/V1.2 收口）。
 *
 * <p>访问路径：
 * <ul>
 *   <li>/v3/api-docs — OpenAPI 3.0 JSON</li>
 *   <li>/doc.html — knife4j 增强 UI</li>
 *   <li>/swagger-ui/index.html — 标准 Swagger UI</li>
 * </ul>
 *
 * <h3>两类安全模式</h3>
 * <ul>
 *   <li><b>Sa-Token</b>（管理后台）：HTTP header {@code Authorization: Bearer <satoken>}</li>
 *   <li><b>OpenAPI 签名</b>（第三方平台）：4 头 X-Openapi-Access-Key / Timestamp / Nonce / Signature
 *       — 见 {@code OpenapiAuthInterceptor}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI v4MvpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MasterLife V4 API")
                        .description("AI 驱动的房地产智能协作平台 — 全 RESTful API。\n\n"
                                + "**模块前缀约定**：\n"
                                + "- `/admin/**` — 管理后台 Sa-Token 鉴权\n"
                                + "- `/app/**` — App/小程序 Sa-Token 鉴权\n"
                                + "- `/openapi/**` — 第三方开发者平台（AK/SK + HMAC-SHA256 签名）\n"
                                + "- `/api/**` — 公开端点（如 i18n 词条、分享链接、Webhook 回调）")
                        .version("4.0.0")
                        .contact(new Contact().name("MasterLife").email("support@pengchengkeji.com"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("/").description("当前部署环境")))
                .components(new Components()
                        .addSecuritySchemes("Sa-Token", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Sa-Token Bearer Token，登录后通过 /admin/auth/login 获取"))
                        .addSecuritySchemes("OpenAPI-Signature", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Openapi-Signature")
                                .description("HMAC-SHA256 签名（详见 /openapi/** 端点说明）")))
                .addSecurityItem(new SecurityRequirement().addList("Sa-Token"));
    }
}
