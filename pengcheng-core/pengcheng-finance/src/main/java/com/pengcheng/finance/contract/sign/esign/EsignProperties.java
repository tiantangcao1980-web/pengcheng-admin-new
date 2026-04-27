package com.pengcheng.finance.contract.sign.esign;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * e签宝对接配置属性。
 * <p>
 * 在 application.yml 中添加：
 * <pre>
 * pengcheng:
 *   esign:
 *     app-id: YOUR_APP_ID
 *     app-secret: YOUR_APP_SECRET
 *     # 沙箱环境: https://openapi.esign.cn
 *     api-host: https://smlopenapi.esign.cn
 *     callback-url: https://your-domain.com/webhook/esign/notify
 * pengcheng:
 *   feature:
 *     esign: true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "pengcheng.esign")
public class EsignProperties {

    /** e签宝开放平台 App ID */
    private String appId;

    /** e签宝开放平台 App Secret（用于 HMAC-SHA256 签名） */
    private String appSecret;

    /**
     * e签宝 API 主机地址。
     * <ul>
     *   <li>正式环境：https://smlopenapi.esign.cn</li>
     *   <li>沙箱环境：https://openapi.esign.cn</li>
     * </ul>
     */
    private String apiHost = "https://smlopenapi.esign.cn";

    /** 签署完成后 e签宝回调的 Webhook 地址（需公网可达） */
    private String callbackUrl;
}
