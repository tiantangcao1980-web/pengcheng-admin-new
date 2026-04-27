package com.pengcheng.job.task;

import com.pengcheng.oa.flow.service.ApprovalFlowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * V4 MVP 闭环② — 审批超时扫描定时任务。
 *
 * <p>每 5 分钟扫描一次 {@code approval_instance.current_node_deadline} 已过期的实例，
 * 调用 {@link ApprovalFlowEngine#sweepTimeouts()} 推进或终结。
 *
 * <p>开关：{@code pengcheng.oa.approval.timeout-sweep-enabled=false} 可禁用（测试或临时停机）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "pengcheng.oa.approval.timeout-sweep-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ApprovalTimeoutSweepJob {

    private final ApprovalFlowEngine approvalFlowEngine;

    /** 5 分钟一次 */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void sweep() {
        try {
            int processed = approvalFlowEngine.sweepTimeouts();
            if (processed > 0) {
                log.info("[ApprovalTimeoutSweep] 推进/终结超时审批实例 {} 个", processed);
            }
        } catch (Exception e) {
            log.error("[ApprovalTimeoutSweep] 审批超时扫描失败", e);
        }
    }
}
