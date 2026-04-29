package com.pengcheng.finance.contract.sign.esign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * e签宝自动配置类。
 * <p>
 * 默认关闭，仅当 {@code pengcheng.feature.esign=true} 时激活。
 * 关闭时不注册任何 Bean，{@code ContractServiceImpl} 的 {@code @Autowired(required=false)} 字段为 null，
 * 调用时抛出 {@link IllegalStateException} 提示用户开启 Feature Flag。
 *
 * <h3>启用步骤</h3>
 * <pre>
 * # application.yml / application-prod.yml
 * pengcheng:
 *   feature:
 *     esign: true
 *   esign:
 *     app-id: ${ESIGN_APP_ID}          # 从环境变量注入，不要硬编码
 *     app-secret: ${ESIGN_APP_SECRET}
 *     api-host: https://smlopenapi.esign.cn
 *     callback-url: https://your-domain.com/webhook/esign/notify
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "pengcheng.feature.esign", havingValue = "true")
@EnableConfigurationProperties(EsignProperties.class)
public class EsignAutoConfiguration {

    @Bean
    public EsignHttpClient esignHttpClient(EsignProperties properties) {
        return new HutoolEsignHttpClient(properties);
    }
}
