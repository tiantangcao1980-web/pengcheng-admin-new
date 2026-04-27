package com.pengcheng.admin.controller.finance;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.finance.contract.service.impl.ContractServiceImpl;
import com.pengcheng.finance.contract.sign.esign.EsignProperties;
import com.pengcheng.finance.contract.sign.esign.dto.EsignCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * e签宝 Webhook 回调接收 Controller。
 * <p>
 * 路由：{@code POST /webhook/esign/notify}
 * <p>
 * 安全机制：
 * <ol>
 *   <li>签名校验：{@code HMAC-SHA256(rawBody, appSecret)} == 请求头
 *       {@code X-Tsign-Open-SIGNATURE}（均 Base64 编码）</li>
 *   <li>幂等去重：Redis key = {@code esign:callback:{eventId}}，TTL 24h，
 *       相同 eventId 只处理一次。</li>
 * </ol>
 *
 * <p>仅在 {@code pengcheng.feature.esign=true}（即 {@link EsignProperties} Bean 存在）时注册。
 */
@Slf4j
@RestController
@RequestMapping("/webhook/esign")
@RequiredArgsConstructor
@ConditionalOnBean(EsignProperties.class)
public class EsignWebhookController {

    /** Redis 回调去重 key 前缀 */
    private static final String CALLBACK_DEDUP_PREFIX = "esign:callback:";

    /** 回调去重 TTL（小时） */
    private static final long DEDUP_TTL_HOURS = 24L;

    private final EsignProperties esignProperties;
    private final ContractServiceImpl contractService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 接收 e签宝 Webhook 通知。
     *
     * @param rawBody   请求原始 body（字符串，供签名校验）
     * @param signature 请求头 {@code X-Tsign-Open-SIGNATURE}
     * @return 200 OK（e签宝要求返回 200，否则会重试）
     */
    @PostMapping("/notify")
    public ResponseEntity<String> notify(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Tsign-Open-SIGNATURE", required = false) String signature) {

        // 1. 签名校验
        if (!verifySignature(rawBody, signature)) {
            log.warn("[Esign] Webhook 签名校验失败 signature={}", signature);
            // 返回 200 避免 e签宝无限重试，但记录警告日志
            return ResponseEntity.ok("signature invalid");
        }

        // 2. 解析 JSON
        EsignCallback callback;
        try {
            callback = objectMapper.readValue(rawBody, EsignCallback.class);
        } catch (Exception e) {
            log.error("[Esign] Webhook body 解析失败 body={}", rawBody, e);
            return ResponseEntity.ok("parse error");
        }

        // 3. 幂等去重（Redis SETNX）
        String eventId = callback.getEventId();
        if (eventId != null && !eventId.isEmpty()) {
            String dedupKey = CALLBACK_DEDUP_PREFIX + eventId;
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(isNew)) {
                log.info("[Esign] Webhook 重复回调，跳过处理 eventId={}", eventId);
                return ResponseEntity.ok("duplicated");
            }
        }

        // 4. 业务处理
        try {
            contractService.handleSignCallback(callback);
        } catch (Exception e) {
            log.error("[Esign] Webhook 业务处理异常 action={} signFlowId={}",
                    callback.getAction(), callback.getSignFlowId(), e);
            // 清除去重 key，允许 e签宝重试
            if (eventId != null && !eventId.isEmpty()) {
                redisTemplate.delete(CALLBACK_DEDUP_PREFIX + eventId);
            }
            // 返回 200 以避免 e签宝短时间内大量重试，业务异常需通过日志/告警跟进
            return ResponseEntity.ok("error");
        }

        return ResponseEntity.ok("success");
    }

    /**
     * 校验 e签宝 Webhook 签名。
     * <p>
     * 算法：{@code Base64(HMAC-SHA256(rawBody, appSecret))} == signature header。
     *
     * @param rawBody   请求原始 body（UTF-8）
     * @param signature 请求头 X-Tsign-Open-SIGNATURE 值
     * @return true 表示签名合法
     */
    boolean verifySignature(String rawBody, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            String secret = esignProperties.getAppSecret();
            if (secret == null || secret.isEmpty()) {
                log.warn("[Esign] appSecret 未配置，跳过签名校验");
                return true; // 开发环境未配置时放行，生产必须配置
            }
            HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
            byte[] computed = hmac.digest(rawBody.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.encode(computed);
            return expected.equals(signature);
        } catch (Exception e) {
            log.error("[Esign] 签名校验异常", e);
            return false;
        }
    }
}
