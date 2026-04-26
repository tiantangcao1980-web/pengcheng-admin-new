package com.pengcheng.ai.asr;

import lombok.Data;

/**
 * ASR 转写请求。
 * <p>
 * APP 原生录音 → 上传至云存储拿到 audioUrl → 调用 /admin/ai/asr/transcribe →
 * 后端调用 DashScope ASR（或现有 AiMultiModalService）→ 返回纯文本。
 */
@Data
public class AsrRequest {
    /** 音频文件 URL（已上传到 OSS / 本地静态目录） */
    private String audioUrl;

    /** 音频格式：mp3/wav/m4a/aac/amr 等（DashScope 大部分支持） */
    private String format;

    /** 采样率（Hz），可选 */
    private Integer sampleRate;

    /** 语言：zh / en，默认 zh */
    private String language;

    /** 关联会话（可选，便于审计） */
    private String conversationId;
}
