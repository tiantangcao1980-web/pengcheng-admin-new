package com.pengcheng.system.ocr.baidu;

import com.pengcheng.system.ocr.OcrProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 百度 OCR 自动装配
 *
 * <h3>开启方式（application.yml）</h3>
 * <pre>
 * pengcheng:
 *   feature:
 *     ocr:
 *       baidu: true          # 默认 false，设为 true 才激活此配置
 *   ocr:
 *     baidu:
 *       api-key: YOUR_API_KEY
 *       secret-key: YOUR_SECRET_KEY
 * </pre>
 *
 * <p>当 {@code pengcheng.feature.ocr.baidu=false}（或未配置）时，
 * 整个 Configuration 不生效，Spring 上下文中不存在 {@link OcrProvider} Bean，
 * 调用方应按需降级（见 {@link com.pengcheng.system.ocr.BusinessCardOcrService}）。</p>
 */
@Configuration
@ConditionalOnProperty(
        prefix = "pengcheng.feature.ocr",
        name = "baidu",
        havingValue = "true",
        matchIfMissing = false
)
@EnableConfigurationProperties(BaiduOcrProperties.class)
public class BaiduOcrAutoConfiguration {

    /**
     * Hutool HTTP 客户端 Bean
     */
    @Bean
    public BaiduOcrHttpClient baiduOcrHttpClient(BaiduOcrProperties properties) {
        return new HutoolBaiduOcrHttpClient(properties.getTimeoutMs());
    }

    /**
     * 百度名片 OCR Provider Bean
     *
     * <p>注入 {@code enabled=true}（已由 @ConditionalOnProperty 保证）。</p>
     */
    @Bean
    public OcrProvider baiduBusinessCardOcrProvider(BaiduOcrProperties properties,
                                                    BaiduOcrHttpClient baiduOcrHttpClient) {
        return new BaiduBusinessCardOcrProvider(properties, baiduOcrHttpClient, true);
    }
}
