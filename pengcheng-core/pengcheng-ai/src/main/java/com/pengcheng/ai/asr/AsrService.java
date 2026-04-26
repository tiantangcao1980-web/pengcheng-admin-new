package com.pengcheng.ai.asr;

/**
 * 语音转写服务接口（V4.0 MVP 闭环④ 语音输入）。
 *
 * <p>默认实现 {@link DashScopeAsrService} 在未配置 DashScope token 时
 * 自动降级为 {@code provider=mock} 的占位实现，方便前端联调，不阻塞业务。
 */
public interface AsrService {

    /**
     * 同步转写。
     */
    AsrResponse transcribe(AsrRequest request);
}
