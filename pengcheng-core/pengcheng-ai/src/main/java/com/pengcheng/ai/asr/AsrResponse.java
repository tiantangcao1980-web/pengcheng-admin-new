package com.pengcheng.ai.asr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsrResponse {
    /** 转写后的纯文本 */
    private String transcript;

    /** 实际耗时（毫秒） */
    private Long latencyMs;

    /** 转写引擎来源：dashscope / mock */
    private String provider;
}
