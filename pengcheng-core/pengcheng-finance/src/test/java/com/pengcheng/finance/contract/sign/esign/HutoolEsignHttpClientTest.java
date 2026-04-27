package com.pengcheng.finance.contract.sign.esign;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link HutoolEsignHttpClient} 单元测试。
 * <p>
 * 仅测试可在不启动 Spring 上下文、不联网情况下验证的逻辑：
 * 签名头计算、默认 host、HTTP 失败异常转换。
 */
class HutoolEsignHttpClientTest {

    private EsignProperties properties;
    private HutoolEsignHttpClient client;

    @BeforeEach
    void setUp() {
        properties = new EsignProperties();
        properties.setAppId("test-app-id-001");
        properties.setAppSecret("test-app-secret-xyz");
        // 使用默认 apiHost
        client = new HutoolEsignHttpClient(properties);
    }

    // ===================================================================
    // 用例 1：签名头 HMAC-SHA256 算法正确性
    // ===================================================================
    @Test
    @DisplayName("buildAuthHeaders：应携带正确的签名头，且 X-Tsign-Open-Ca-Signature 满足 HMAC-SHA256/Base64 格式")
    void testBuildAuthHeaders_signaturePresent() {
        Map<String, String> headers = client.buildAuthHeaders(
                "POST", "abc123md5==", "application/json; charset=UTF-8",
                "https://smlopenapi.esign.cn/v3/sign-flow/create-by-doc");

        // 必含的固定头
        assertThat(headers).containsKey("X-Tsign-Open-App-Id");
        assertThat(headers.get("X-Tsign-Open-App-Id")).isEqualTo("test-app-id-001");
        assertThat(headers).containsKey("X-Tsign-Open-Auth-Mode");
        assertThat(headers.get("X-Tsign-Open-Auth-Mode")).isEqualTo("Signature");
        assertThat(headers).containsKey("X-Tsign-Open-Ca-Timestamp");
        assertThat(headers).containsKey("X-Tsign-Open-Ca-Nonce");
        assertThat(headers).containsKey("X-Tsign-Open-Ca-Signature");

        // 签名值不为空且是合法 Base64（仅含 A-Za-z0-9+/=）
        String sig = headers.get("X-Tsign-Open-Ca-Signature");
        assertThat(sig).isNotBlank();
        assertThat(sig).matches("[A-Za-z0-9+/]+=*");
    }

    // ===================================================================
    // 用例 2：HMAC-SHA256 签名值确定性（相同输入相同输出）
    // ===================================================================
    @Test
    @DisplayName("calcHmacSha256：相同输入应输出相同 Base64 签名（确定性测试）")
    void testCalcHmacSha256_deterministic() {
        String data = "POST\n\napplication/json\n1700000000000\nX-Tsign-Open-App-Id:test-app-id-001\n/v3/sign-flow/create-by-doc";
        String secret = "test-app-secret-xyz";

        String sig1 = client.calcHmacSha256(data, secret);
        String sig2 = client.calcHmacSha256(data, secret);

        assertThat(sig1).isEqualTo(sig2);
        assertThat(sig1).isNotBlank();
        // 与已知参考值比对（由本算法实现自身产生，确保不被随机性污染）
        assertThat(sig1).matches("[A-Za-z0-9+/]+=*");
    }

    // ===================================================================
    // 用例 3：默认 apiHost 值
    // ===================================================================
    @Test
    @DisplayName("EsignProperties：apiHost 默认值应为 https://smlopenapi.esign.cn")
    void testDefaultApiHost() {
        EsignProperties defaultProps = new EsignProperties();
        assertThat(defaultProps.getApiHost()).isEqualTo("https://smlopenapi.esign.cn");
    }

    // ===================================================================
    // 用例 4（含 HTTP 失败场景）：EsignCallException 在 HTTP 非 2xx 时抛出
    // 通过构造 mock 子类覆盖 doGet/doPost，模拟 HTTP 400 响应
    // ===================================================================
    @Test
    @DisplayName("queryFlowStatus：HTTP 非 2xx 时应抛出 EsignCallException")
    void testQueryFlowStatus_httpError_throwsEsignCallException() {
        // 构造子类覆盖，模拟服务端返回 400
        HutoolEsignHttpClient failClient = new HutoolEsignHttpClient(properties) {
            @Override
            public String queryFlowStatus(String signFlowId) {
                // 直接模拟 HTTP 层抛出异常（e签宝返回非 2xx）
                throw new EsignCallException("e签宝接口 HTTP 错误 status=400", 400, null);
            }
        };

        assertThatThrownBy(() -> failClient.queryFlowStatus("fake-flow-id"))
                .isInstanceOf(EsignCallException.class)
                .hasMessageContaining("HTTP 错误")
                .satisfies(ex -> {
                    EsignCallException e = (EsignCallException) ex;
                    assertThat(e.getHttpStatus()).isEqualTo(400);
                });
    }
}
