package com.pengcheng.realty.receivable.task;

import com.pengcheng.realty.receivable.service.ReceivableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 回款逾期巡检定时任务
 *
 * 通过 Quartz SysJob 配置触发：
 *   bean: receivableOverdueJob
 *   method: execute
 *   建议 cron: 0 0 9 * * ?  （每天 9:00，避免凌晨员工无法响应）
 *
 * 职责：调用 ReceivableService.runOverdueCheck() 扫描所有未结清分期：
 *   - 更新分期状态（NOT_DUE / PENDING / PARTIAL / OVERDUE）
 *   - 按 T+0 / T+3 / T+7 / T+15 四档触发逾期告警
 *   - 即将到期（3 天内）发"到期提醒"
 *   - 升档时发布 ReceivableOverdueAlertEvent，由订阅者发站内信 / 微信推送
 */
@Slf4j
@Component("receivableOverdueJob")
@RequiredArgsConstructor
public class ReceivableOverdueJob {

    private final ReceivableService receivableService;

    public void execute() {
        log.info("[回款巡检] 任务启动");
        try {
            int[] result = receivableService.runOverdueCheck();
            log.info("[回款巡检] 任务完成，逾期触发通知 {} 条，到期提醒 {} 条", result[0], result[1]);
        } catch (Exception e) {
            log.error("[回款巡检] 任务执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
