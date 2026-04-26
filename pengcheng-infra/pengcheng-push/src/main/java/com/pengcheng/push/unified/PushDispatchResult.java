package com.pengcheng.push.unified;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一推送调度结果
 *
 * <p>包含决策出的通道、是否真正下发成功（具体推送服务返回值）以及失败原因。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushDispatchResult {

    /** 决策选中的通道（即便 NONE 也会写日志） */
    private PushChannel channel;

    /** 下发是否成功 */
    private boolean success;

    /** 失败/降级原因，便于排查（脱敏后） */
    private String reason;

    public static PushDispatchResult ok(PushChannel channel) {
        return PushDispatchResult.builder().channel(channel).success(true).build();
    }

    public static PushDispatchResult fail(PushChannel channel, String reason) {
        return PushDispatchResult.builder().channel(channel).success(false).reason(reason).build();
    }
}
