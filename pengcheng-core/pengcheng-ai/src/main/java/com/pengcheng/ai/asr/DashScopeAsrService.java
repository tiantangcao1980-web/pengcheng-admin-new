package com.pengcheng.ai.asr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * DashScope ASR 实现（V4.0 MVP 阶段为 mock 桩，留 TODO 标识）。
 *
 * <p>真正接入 DashScope 时：
 * <pre>
 *   1. 在 application.yml 配置 spring.ai.dashscope.api-key
 *   2. 引入 spring-ai-alibaba-starter-dashscope（已在 pom 中）
 *   3. 用 com.alibaba.dashscope.audio.asr.recognition.Recognition 做异步识别
 *   4. 替换 transcribe() 方法体，去掉 mock 分支
 * </pre>
 *
 * <p>当前若 audioUrl 为空 → 抛参数异常；否则返回 mock 结果以便前端联调。
 * mock 结果包含 audioUrl 的最后一段路径，便于联调期断言。
 */
@Slf4j
@Service
public class DashScopeAsrService implements AsrService {

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Override
    public AsrResponse transcribe(AsrRequest request) {
        if (request == null || request.getAudioUrl() == null || request.getAudioUrl().isBlank()) {
            throw new IllegalArgumentException("audioUrl required");
        }
        long start = System.currentTimeMillis();
        // TODO(V4.0 D4): 接入 DashScope Recognition API
        //  Recognition recognition = new Recognition();
        //  RecognitionParam param = RecognitionParam.builder()
        //          .apiKey(apiKey)
        //          .model("paraformer-realtime-v2")
        //          .format(request.getFormat())
        //          .sampleRate(request.getSampleRate())
        //          .build();
        //  String transcript = recognition.callFile(param, request.getAudioUrl());
        //  return new AsrResponse(transcript, System.currentTimeMillis() - start, "dashscope");
        if (apiKey == null || apiKey.isBlank()) {
            log.info("[asr] dashscope api key not configured, returning mock for url={}", request.getAudioUrl());
        }
        String mockText = "[mock-asr] 已收到音频 " + tailPath(request.getAudioUrl())
                + "，请在 spring.ai.dashscope.api-key 配置后启用真实转写";
        return new AsrResponse(mockText, System.currentTimeMillis() - start, "mock");
    }

    private String tailPath(String url) {
        if (url == null) {
            return "";
        }
        int q = url.indexOf('?');
        String path = q > 0 ? url.substring(0, q) : url;
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
